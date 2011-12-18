//
// React - a library for functional-reactive-like programming
// Copyright (c) 2011, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/react/blob/master/LICENSE

#import <Foundation/Foundation.h>
#import "RAReactor.h"

@class RAConnectionGroup;

@interface RAUnitSignal : RAReactor
- (void) emit;
- (RAConnection*) connectUnit:(RAUnitBlock)block;
- (RAConnection*) withPriority:(int)priority connectUnit:(RAUnitBlock)block;
@end
