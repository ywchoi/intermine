package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.util.PropertiesUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.StringInputStream;

import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Create a the Lucene keyword search index for a mine.
 * @author Alex Kalderimis
 */
public class SummariseObjectstoreProcess extends PostProcessor
{
    /**
     * Create a new instance of CreateSearchIdexProcess
     *
     * @param osw object store writer
     */
    public SummariseObjectstoreProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     */
    public void postProcess()
            throws ObjectStoreException {
        System.out .println("summarising objectstore ...");
        ObjectStore os = osw.getObjectStore();
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("objectstoresummary.config.properties"));
        } catch (IOException e) {
            throw new BuildException("Could not open the class keys");
        } catch (NullPointerException e) {
            throw new BuildException("Could not find the class keys");
        }
        try {
            ObjectStoreSummary oss = new ObjectStoreSummary(os, props);
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            MetadataManager.store(db, MetadataManager.OS_SUMMARY,
                    PropertiesUtil.serialize(oss.toProperties()));
        } catch (ClassNotFoundException e) {
            throw new BuildException("Could not find the class keys" + e);
        } catch (IOException e) {
            throw new BuildException("Could not open the class keys");
        } catch (SQLException e) {
            throw new BuildException("Could not find the class keys " + e);
        }
    }
}
