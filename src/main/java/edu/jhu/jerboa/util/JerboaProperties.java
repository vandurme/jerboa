// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 26 Oct 2010

package edu.jhu.jerboa.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.jhu.jerboa.NoJerboaPropertyException;

/**
 * A utility wrapper around {@link java.util.Properties}, supporting type
 * specific querying on property values, and throwing Exception when properties
 * are not found in cases with no default value.
 * <p>
 * Uses System level properties if they exist, then checks the property file
 * that was loaded into this object as backup (so command line specified
 * properties can supersede a configuration file).
 * <p>
 * Contains basic functionality for referencing other system properties, meant
 * for things like setting a root directory just once, and making other values
 * relative to that. Syntax is: (via example)
 * <p>
 * ROOT = /home/joe/project Data = {ROOT}/data
 * 
 * @author Benjamin Van Durme
 * 
 */
public class JerboaProperties {

    private static JProperties properties = new JProperties();
    private static boolean isInitialized = false;

    private JerboaProperties() {

    }

    public static void initializeConfig(String pathToAnalyticsProperties, boolean loadFromClasspath) throws IOException {
        // if previously initialized, just return. we're already done.
        if (isInitialized)
            return;
        // if load to classpath is true, attempt to load from classpath.
        if (loadFromClasspath) {
            BufferedReader br = new BufferedReader(new InputStreamReader(JerboaProperties.class.getClassLoader().getResourceAsStream(
                    pathToAnalyticsProperties)));
            properties.load(br);
            isInitialized = true;
            br.close();
        } else {
            // otherwise, find the file on disk and load it.
            FileReader fr = new FileReader(new File(pathToAnalyticsProperties));
            properties.load(fr);
            fr.close();
        }
    }

    public static double getDouble(String key) throws NoJerboaPropertyException {
        String value = getProperty(key);
        if (value == null)
            throw new NoJerboaPropertyException(key);
        else
            return Double.parseDouble(value);
    }

    public static double getDouble(String key, double defaultValue) {
        String value = getProperty(key);
        if (value == null)
            return defaultValue;
        else
            return Double.parseDouble(value);
    }

    public static double getDoubleOrNull(String key) {
        String value = getProperty(key);
        return value == null ? null : Double.parseDouble(value);
    }

    public static long getLong(String key) throws NoJerboaPropertyException {
        String value = getProperty(key);
        if (value == null)
            throw new NoJerboaPropertyException(key);
        else
            return Long.parseLong(value);
    }

    public static long getLong(String key, long defaultValue) {
        String value = getProperty(key);
        if (value == null)
            return defaultValue;
        else
            return Long.parseLong(value);
    }

    public static long getLongOrNull(String key) {
        String value = getProperty(key);
        return value == null ? null : Long.parseLong(value);
    }

    public static int getInt(String key) throws NoJerboaPropertyException {
        String value = getProperty(key);
        if (value == null)
            throw new NoJerboaPropertyException(key);
        else
            return Integer.parseInt(value);
    }

    public static int getInt(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null)
            return defaultValue;
        else
            return Integer.parseInt(value);
    }

    public static int getIntOrNull(String key) {
        String value = getProperty(key);
        return value == null ? null : Integer.parseInt(value);
    }

    public static boolean getBoolean(String key) throws NoJerboaPropertyException {
        String value = getProperty(key);
        if (value == null)
            throw new NoJerboaPropertyException(key);
        else
            return Boolean.parseBoolean(value);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value == null)
            return defaultValue;
        else
            return Boolean.parseBoolean(value);
    }

    public static boolean getBooleanOrNull(String key) {
        String value = getProperty(key);
        return value == null ? null : Boolean.parseBoolean(value);
    }

    public static String[] getStrings(String key) throws NoJerboaPropertyException {
        String value = getProperty(key);
        if (value == null)
            throw new NoJerboaPropertyException(key);
        else
            return value.split("\\s");
    }

    public static String[] getStrings(String key, String[] defaultValue) {
        String value = getProperty(key);
        if (value == null)
            return defaultValue;
        else
            return value.split("\\s");
    }

    public static String[] getStringsOrNull(String key) {
        String value = getProperty(key);
        return value == null ? null : value.split("\\s");
    }

    public static String getProperty(String key) {
        // check system properties first
        String value = System.getProperty(key);
        if (value == null) {
            // wasn't in system properties - check loaded properties object
            value = properties.getProperty(key);
        }

        // we will return null, and let caller deal w/ it
        return value;
    }

    public static String getProperty(String key, String defaultValue) {
        String value = JerboaProperties.getProperty(key);
        if (value == null)
            // we'll return the default if we didn't get anything from system or
            // loaded properties
            return defaultValue;
        else
            return value;
    }
}
