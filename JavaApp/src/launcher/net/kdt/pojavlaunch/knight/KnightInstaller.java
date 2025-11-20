
public class KnightInstaller implements Runnable {

    // private static final String HEAD_N = "`head -n ";
    private static final String CODE_LINE = "code = ";
    private static final String ARG_LINE = "jvmarg = ";
    private static final String CLASS_LINE = "class = ";

    private final Progress pr;
    private final File destination;
    private final File spiral;
    private int progressStep = 0;

    public KnightInstaller(Progress pr) {
        this.pr = pr;
        // Use Tools.DIR_GAME_HOME which should be initialized by the app
        this.destination = new File(Tools.DIR_GAME_HOME);
        this.spiral = new File(destination, "spiral"); // Assuming spiral dir is inside game home or similar
    }

    @Override
    public void run() {
        try {
            pr.moveToTop();
            pr.postStepProgress(++progressStep);

            pr.postLogLine("Generating JSON...", null);
            List<String> codeJars = new ArrayList<>();
            List<String> jvmArgs = new ArrayList<>();
            String mainClass = null;

            File getdownFile = new File(spiral, "getdown.txt");
            if (!getdownFile.exists()) {
                // Fallback or error? Assuming it exists for now or we might need to download
                // it?
                // The original code assumed it exists in 'spiral' dir.
                // If spiral dir is not set, we might need to set it.
                // In PojavLauncher.java we set appdir to ./spiral
            }

            BufferedReader rdr = new BufferedReader(
                    new InputStreamReader(new FileInputStream(getdownFile)));
            String line;
            while ((line = rdr.readLine()) != null) {
                if (line.startsWith(CODE_LINE)) {
                    String codeJar = line.substring(CODE_LINE.length());
                    if (!codeJar.contains("jinput") && !codeJar.contains("lwjgl")) {
                        codeJars.add(codeJar);
                    }
                } else if (line.startsWith(ARG_LINE)) {
                    String arg = line.substring(ARG_LINE.length());
                    if (!arg.startsWith("-Xm") && !arg.startsWith("-Djava.library.path") && !arg.startsWith("[")) {
                        jvmArgs.add(arg);
                    }
                } else if (line.startsWith(CLASS_LINE)) {
                    mainClass = line.substring(CLASS_LINE.length());
                }
            }
            rdr.close();

            jvmArgs.add("-Dorg.lwjgl.opengl.disableStaticInit=true");
            JSONObject outputJson = new JSONObject();
            outputJson.put("minecraftArguments", "");
            for (String s : codeJars) {
                File source = new File(spiral, s);
                String fileName = source.getName();
                String extension = fileName.substring(fileName.lastIndexOf("."));
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                String libName = "spiral:" + fileName + ":0.0";
                File libDestination = new File(destination,
                        "libraries/spiral/" + fileName + "/0.0/" + fileName + "-0.0" + extension);
                libDestination.getParentFile().mkdirs();
                if (source.exists()) {
                    Files.copy(source.toPath(), libDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                JSONObject library = new JSONObject();
                library.put("name", libName);
                outputJson.append("libraries", library);
            }
            outputJson.put("id", "SpiralKnights");
            outputJson.put("releaseTime", "2009-05-13T20:11:00+00:00");
            outputJson.put("time", "2009-05-13T20:11:00+00:00");
            outputJson.put("type", "release");
            outputJson.put("mainClass", mainClass);
            File versionPath = new File(destination, "versions/SpiralKnights/SpiralKnights.json");
            versionPath.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(versionPath)) {
                fos.write(outputJson.toString().getBytes());
            }

            String sprofiles = null;
            String b64Default = null;
            try {
                byte[] bprofiles = Files.readAllBytes(new File(destination, "launcher_profiles.json").toPath());
                sprofiles = new String(bprofiles, 0, bprofiles.length);
            } catch (Exception ignored) {
            }
            try {
                b64Default = Base64.getEncoder()
                        .encodeToString(Files.readAllBytes(new File(spiral, "desktop.png").toPath()));
            } catch (Exception ignored) {
            }

            JSONObject profiles = sprofiles == null ? new JSONObject() : new JSONObject(sprofiles);
            JSONObject spiralKnightsProfile = new JSONObject();
            StringBuilder sb = new StringBuilder();
            int sz = jvmArgs.size();
            for (int i = 0; i < sz; i++) {
                sb.append(jvmArgs.get(i).replace("%APPDIR%", "./spiral/"));
                if (i < sz - 1) {
                    sb.append(" ");
                }
            }
            spiralKnightsProfile.put("javaArgs", sb.toString());
            spiralKnightsProfile.put("lastVersionId", "SpiralKnights");
            spiralKnightsProfile.put("name", "Spiral Knights");
            if (b64Default != null) {
                spiralKnightsProfile.put("icon", "data:image/png;base64," + b64Default);
            }
            if (profiles.has("profiles")) {
                profiles.getJSONObject("profiles").put("SpiralKnights", spiralKnightsProfile);
            } else {
                JSONObject newProfiles = new JSONObject();
                newProfiles.put("SpiralKnights", spiralKnightsProfile);
                profiles.put("profiles", newProfiles);
            }

            Files.write(new File(destination, "launcher_profiles.json").toPath(), profiles.toString().getBytes());
            pr.postStepProgress(++progressStep);
            pr.postLogLine("All done!", null);
            pr.setPartIndeterminate(false);
            pr.unlockExit();

        } catch (Exception e) {
            pr.postLogLine("Failed to install Spiral Knights", e);
            pr.setPartIndeterminate(false);
            pr.unlockExit();
        }
    }
}
