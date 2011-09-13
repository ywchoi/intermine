package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Publication;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Display publication and number of genes annotated by that publication.  order ASC
 *
 * @author Julie Sullivan
 */
public class PublicationCountsDisplayer extends ReportDisplayer
{


    protected static final Logger LOG = Logger.getLogger(PublicationCountsDisplayer.class);

    /**
     * Construct with config and the InterMineAPI.
     *
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public PublicationCountsDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        Map<Publication, String> publications = new LinkedHashMap<Publication, String>();
        InterMineObject object = reportObject.getObject();
        String type = DynamicUtil.getSimpleClass(object).getSimpleName();
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Query q = getQuery(im, object.getId(), type);
        ObjectStore os = im.getObjectStore();
        Results results = os.execute(q, 2000, true, true, true);
        Iterator<Object> it = results.iterator();
        while (it.hasNext()) {
            ResultsRow rr =  (ResultsRow) it.next();
            Publication pub = (Publication) rr.get(0);
            Long count = (Long) rr.get(1);
            publications.put(pub, count.toString());
        }
        request.setAttribute("results", publications);
        if (results.isEmpty()) {
            request.setAttribute("noResults", "No publications found");
        }
    }

    private Query getQuery(InterMineAPI im, Integer geneID, String type) {
        Model model = im.getModel();
        QueryClass qcReportGene = null;
        QueryClass qcPub = new QueryClass(Publication.class);
        QueryClass qcOtherGenes = null;

        try {
            qcReportGene = new QueryClass(Class.forName(model.getPackageName()  + "." + type));
            qcOtherGenes = new QueryClass(Class.forName(model.getPackageName()  + "." + type));
        } catch (ClassNotFoundException e) {
            LOG.error("Error rendering publication count displayer", e);
            return null;
        }

        QueryField qfReportGeneId = new QueryField(qcReportGene, "id");

        // constraints
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        cs.addConstraint(new SimpleConstraint(qfReportGeneId, ConstraintOp.EQUALS,
                new QueryValue(geneID)));

        // gene.publication = publication
        QueryCollectionReference qcr1 = new QueryCollectionReference(qcReportGene, "publications");
        cs.addConstraint(new ContainsConstraint(qcr1, ConstraintOp.CONTAINS, qcPub));

        QueryCollectionReference qcr2 = new QueryCollectionReference(qcOtherGenes, "publications");
        cs.addConstraint(new ContainsConstraint(qcr2, ConstraintOp.CONTAINS, qcPub));

        Query q = new Query();
        q.setDistinct(true);

        // from statement
        q.addFrom(qcReportGene);
        q.addFrom(qcPub);
        q.addFrom(qcOtherGenes);

        // add constraints to query
        q.setConstraint(cs);

        q.addToSelect(qcPub);

        QueryFunction qf = new QueryFunction();
        q.addToSelect(qf);

        q.addToGroupBy(qcPub);

        q.addToOrderBy(qf);
        return q;
    }
}
