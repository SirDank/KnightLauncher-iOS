#import "LauncherMenuViewController.h"
#import "AFNetworking.h"
#import "ALTServerConnection.h"
#import "LauncherNavigationController.h"
#import "LauncherNewsViewController.h"
#import "LauncherPreferences.h"
#import "LauncherPreferencesViewController.h"
#import "PLProfiles.h"
#import "UIButton+AFNetworking.h"
#import "UIImageView+AFNetworking.h"
#import "UIKit+hook.h"
#import "authenticator/BaseAuthenticator.h"
#import "ios_uikit_bridge.h"
#import "utils.h"

#include <dlfcn.h>

@implementation LauncherMenuCustomItem

+ (LauncherMenuCustomItem *)title:(NSString *)title
                        imageName:(NSString *)imageName
                           action:(id)action {
  LauncherMenuCustomItem *item = [[LauncherMenuCustomItem alloc] init];
  item.title = title;
  item.imageName = imageName;
  item.action = action;
  return item;
}

+ (LauncherMenuCustomItem *)vcClass:(Class)class {
  id vc = [class new];
  LauncherMenuCustomItem *item = [[LauncherMenuCustomItem alloc] init];
  item.title = [vc title];
  item.imageName = [vc imageName];
  // View controllers are put into an array to keep its state
  item.vcArray = @[ vc ];
  return item;
}

@end

@interface LauncherMenuViewController ()
@property(nonatomic) NSMutableArray<LauncherMenuCustomItem *> *options;
@property(nonatomic) UILabel *statusLabel;
@property(nonatomic) int lastSelectedIndex;
@property(nonatomic) UIAlertController *progressAlert;
@end

@implementation LauncherMenuViewController

#define contentNavigationController                                            \
  ((LauncherNavigationController *)self.splitViewController.viewControllers[1])

- (void)viewDidLoad {
  [super viewDidLoad];

  [[NSNotificationCenter defaultCenter] addObserver:self
                                           selector:@selector(onProgress:)
                                               name:@"KnightLauncherProgress"
                                             object:nil];

  self.isInitialVc = YES;

  UIImageView *titleView =
      [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"AppLogo"]];
  [titleView setContentMode:UIViewContentModeScaleAspectFit];
  self.navigationItem.titleView = titleView;
  [titleView sizeToFit];

  self.options =
      @[
        [LauncherMenuCustomItem
            vcClass:LauncherPreferencesViewController.class],
      ]
          .mutableCopy;
  if (realUIIdiom != UIUserInterfaceIdiomTV) {
    [self.options
        addObject:(id)[LauncherMenuCustomItem
                          title:localize(@"launcher.menu.custom_controls", nil)
                      imageName:@"MenuCustomControls"
                         action:^{
                           [contentNavigationController
                               performSelector:@selector(enterCustomControls)];
                         }]];
  }
  [self.options
      addObject:(id)[LauncherMenuCustomItem
                        title:@"Install Spiral Knights"
                    imageName:@"MenuInstallJar"
                       action:^{
                         // Trigger installation
                         // We need to call JavaLauncher with a special argument
                         dispatch_async(dispatch_get_global_queue(
                                            DISPATCH_QUEUE_PRIORITY_DEFAULT, 0),
                                        ^{
                                          // This is a bit hacky, we are
                                          // re-using the JavaLauncher
                                          // infrastructure We might need to
                                          // ensure JVM is not already running
                                          // or handle it gracefully For now,
                                          // let's assume it's a fresh start or
                                          // we can just launch it. But wait,
                                          // JavaLauncher.m launches the JVM. If
                                          // it's already running, we can't
                                          // launch it again easily with
                                          // different args if it's a single JVM
                                          // process. However, for the
                                          // installer, we might want to run it.
                                          // If the app is already running, we
                                          // might need to restart it or just
                                          // run the installer if possible.
                                          // Given the constraints, let's try to
                                          // launch it with the special arg.

                                          // Actually, we should check if we can
                                          // just call a static method if JVM is
                                          // running. But for now, let's assume
                                          // we launch it as a "game" with
                                          // special args.

                                          // We need to construct a launch
                                          // target that JavaLauncher
                                          // understands. JavaLauncher expects a
                                          // dictionary or a string (jar path).
                                          // We modified PojavLauncher.java to
                                          // handle "-jar knight_install". So we
                                          // can pass "knight_install" as the
                                          // jar path if we modify
                                          // JavaLauncher.m to handle it, OR we
                                          // can pass a dummy jar path and
                                          // handle it in PojavLauncher.

                                          // Let's pass "knight_install" and
                                          // ensure JavaLauncher doesn't try to
                                          // resolve it as a file if it's that
                                          // specific string. Or better, pass a
                                          // dummy file path that exists, but
                                          // with a special flag? No, let's just
                                          // use the "execute_jar" flow but with
                                          // "knight_install" as the path, and
                                          // we need to update JavaLauncher.m to
                                          // allow this non-existent file if it
                                          // matches our magic string.

                                          // Actually, looking at
                                          // JavaLauncher.m: if ([launchTarget
                                          // isKindOfClass:NSDictionary.class])
                                          // ... else { ... launchJar = YES; }
                                          // if (launchJar) { classpath = ...
                                          // :launchTarget } margv[++margc] =
                                          // "-jar";

                                          // If we pass "knight_install", it
                                          // will try to add it to classpath. If
                                          // it doesn't exist, it might fail or
                                          // just be ignored by java -cp, but
                                          // -jar expects a file.

                                          // So we should probably create a
                                          // dummy jar or just modify
                                          // JavaLauncher.m to handle this case.
                                          // Let's modify JavaLauncher.m first
                                          // to handle "knight_install" special
                                          // case.

                                          launchJVM(nil, @"knight_install", 800,
                                                    600, 8);
                                        });
                       }]];

  // Removed log upload and technoblade tribute for now to clean up

  self.tableView.separatorStyle = UITableViewCellSeparatorStyleNone;

  self.tableView.separatorStyle = UITableViewCellSeparatorStyleNone;

  [self tableView:self.tableView didSelectRowAtIndexPath:indexPath];

  if (getEntitlementValue(@"get-task-allow")) {
    [self displayProgress:localize(@"login.jit.checking", nil)];
    if (isJITEnabled(false)) {
      [self displayProgress:localize(@"login.jit.enabled", nil)];
      [self displayProgress:nil];
    } else {
      [self enableJITWithAltKit];
    }
  } else if (!NSProcessInfo.processInfo.macCatalystApp &&
             !getenv("SIMULATOR_DEVICE_NAME")) {
    [self displayProgress:localize(@"login.jit.fail", nil)];
    [self displayProgress:nil];
    UIAlertController *alert = [UIAlertController
        alertControllerWithTitle:localize(@"login.jit.fail.title", nil)
                         message:localize(
                                     @"login.jit.fail.description_unsupported",
                                     nil)
                  preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *okAction =
        [UIAlertAction actionWithTitle:localize(@"OK", nil)
                                 style:UIAlertActionStyleDefault
                               handler:^(id action) {
                                 exit(-1);
                               }];
    [alert addAction:okAction];
    [self presentViewController:alert animated:YES completion:nil];
  }
}

- (void)displayProgress:(NSString *)status {
  if (status == nil) {
    [(UIActivityIndicatorView *)self.toolbarItems[0].customView stopAnimating];
  } else {
    self.toolbarItems[1].title = status;
  }
}

- (void)enableJITWithAltKit {
  [ALTServerManager.sharedManager startDiscovering];
  [ALTServerManager.sharedManager
      autoconnectWithCompletionHandler:^(ALTServerConnection *connection,
                                         NSError *error) {
        if (error) {
          NSLog(@"[AltKit] Could not auto-connect to server. %@",
                error.localizedRecoverySuggestion);
          [self displayProgress:localize(@"login.jit.fail", nil)];
          [self displayProgress:nil];
        }
        [connection enableUnsignedCodeExecutionWithCompletionHandler:^(
                        BOOL success, NSError *error) {
          if (success) {
            NSLog(@"[AltKit] Successfully enabled JIT compilation!");
            [ALTServerManager.sharedManager stopDiscovering];
            [self displayProgress:localize(@"login.jit.enabled", nil)];
            [self displayProgress:nil];
          } else {
            NSLog(@"[AltKit] Error enabling JIT: %@",
                  error.localizedRecoverySuggestion);
            [self displayProgress:localize(@"login.jit.fail", nil)];
            [self displayProgress:nil];
          }
          [connection disconnect];
        }];
      }];
}

@end
