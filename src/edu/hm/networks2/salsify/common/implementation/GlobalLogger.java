/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hm.networks2.salsify.common.implementation;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author nico
 */
public class GlobalLogger {
    
    private static Logger GLOBAL_LOGGER = null;
    
    private GlobalLogger(String fileNamePrefix) {
        GLOBAL_LOGGER = Logger.getLogger(GlobalLogger.class.getName());
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.mm.yy_hh-mm-ss"));
        try {
            FileHandler fileHandler = new FileHandler("logs" + File.separator + fileNamePrefix + now + ".log");
            fileHandler.setFormatter(new SimpleFormatter());
            GLOBAL_LOGGER.addHandler(fileHandler);
        } catch (IOException exception) {
            System.out.println("IO Exception occured while creating logger! " + exception.toString());
        } catch (SecurityException exception) {
            System.out.println("Security exception occured while creating logger! " + exception.toString());
        }  
    }
    
    /**
     * Get logger instance. File name prefix is only used on first call.
     * 
     * @param fileNamePrefix the prefix of the file to write to.
     * 
     * @return logger instance 
     */
    public static Logger getInstance(String fileNamePrefix) {
        if (GLOBAL_LOGGER == null) {
            // this will intialize our global logger
            new GlobalLogger(fileNamePrefix);
        } 
        return GLOBAL_LOGGER;
    }
    
    /**
     * Get logger instance.
     * 
     * @return logger instance 
     */
    public static Logger getInstance() {
        if (GLOBAL_LOGGER == null) {
            new GlobalLogger("Log_");
        }
        return GLOBAL_LOGGER;
    }
    
}
