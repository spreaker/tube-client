package com.spreaker.tubeclient;

import java.io.File;

/**
 * @author Marco Pracucci <marco.pracucci@spreaker.com>
 */
public abstract class ClientUtil
{
    
    public static String getFileExtension(File file)
    {
        return file.getAbsolutePath().substring(file.getAbsolutePath().length() - 3).toLowerCase();
    }
    
    /**
     * Guess the mimetype based on file extension.
     */
    public static String getFileMime(File file)
    {
        String ext = getFileExtension(file);
        
        if ("aac".equals(ext)) {
            return "audio/aac";
        } if ("mp4".equals(ext)) {
            return "audio/aac-mp4";
        } else {
            return "audio/mpeg";
        }
    }
    
    /**
     * Guess the number of bytes/sec to stream.
     */
    public static int getBytesPerSecond(File file)
    {
        String ext = getFileExtension(file);
        
        if ("aac".equals(ext)) {
            return 8192;
        } if ("mp4".equals(ext)) {
            return 8192;
        } else {
            return 16384;
        }
    }
    
}
