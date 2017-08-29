#import "RCTConvert+GADAdSize.h"

@implementation RCTConvert (GADAdSize)

+ (GADAdSize)GADAdSize:(id)json
{
    NSNumber *widthValue;
    NSNumber *heightValue;
    double width = 0.0;
    double height = 0.0;
    if ((widthValue = [json valueForKey:@"width"])
        && (heightValue = [json valueForKey:@"height"])
        && (width = [widthValue doubleValue])
        && (height = [heightValue doubleValue])) {
        CGSize size = CGSizeMake((CGFloat)width, (CGFloat)height);
        return GADAdSizeFromCGSize(size);
    }
//    if ([adSize isEqualToString:@"banner"]) {
//        return kGADAdSizeBanner;
//    } else if ([adSize isEqualToString:@"fullBanner"]) {
//        return kGADAdSizeFullBanner;
//    } else if ([adSize isEqualToString:@"largeBanner"]) {
//        return kGADAdSizeLargeBanner;
//    } else if ([adSize isEqualToString:@"fluid"]) {
//        return kGADAdSizeFluid;
//    } else if ([adSize isEqualToString:@"skyscraper"]) {
//        return kGADAdSizeSkyscraper;
//    } else if ([adSize isEqualToString:@"leaderboard"]) {
//        return kGADAdSizeLeaderboard;
//    } else if ([adSize isEqualToString:@"mediumRectangle"]) {
//        return kGADAdSizeMediumRectangle;
//    } else if ([adSize isEqualToString:@"smartBannerPortrait"]) {
//        return kGADAdSizeSmartBannerPortrait;
//    } else if ([adSize isEqualToString:@"smartBannerLandscape"]) {
//        return kGADAdSizeSmartBannerLandscape;
//    }
//    else {
//    }
    return kGADAdSizeInvalid;
}

@end
