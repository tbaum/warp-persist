package com.wideplay.warp.persist.spi;

import com.google.inject.matcher.Matcher;

import java.lang.reflect.Method;

/**
 * A class- and methodmatcher that get used together.
 * Used to represent transaction matchers and finder matchers.
 * 
 * @author Robbie Vanbrabant
 */
public interface ClassAndMethodMatcher {

    /**
     * @return the class matcher (never {@code null})
     */
    public Matcher<? super Class<?>> getClassMatcher();

    /**
     * @return @return the method matcher (never {@code null})
     */
    public Matcher<? super Method> getMethodMatcher();
}
