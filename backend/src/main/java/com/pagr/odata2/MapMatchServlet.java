package com.pagr.odata2;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.repackaged.com.google.gson.Gson;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.VoidWork;
import com.pagr.backend.CellUpdate;
import com.pagr.backend.LinkPassage;
import com.pagr.backend.Route;

import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.pagr.backend.OfyService.ofy;

public class MapMatchServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(MapMatchServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        URL url = new URL("http://test.roadmatching.com/rest/mapmatch/?app_id=5f3116c1&app_key=8157023d509d10b058dbf928472536c7&output.waypointsIds=true&output.linkGeometries=true&output.osmProjection=true");
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "text/csv");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(600000);
        connection.setReadTimeout(600000);
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        List<CellUpdate> cellUPdates = ofy().load().type(CellUpdate.class).filter("done", false).list();
        Collections.sort(cellUPdates, new Comparator<CellUpdate>() {
            @Override
            public int compare(CellUpdate o1, CellUpdate o2) {
                return Long.compare(o1.getTimestamp(), o2.getTimestamp());
            }
        });
        for (CellUpdate cellUpdate : cellUPdates) {
            try (PrintWriter pw = new PrintWriter(connection.getOutputStream())) {
                if (cellUpdate.getLatitude() != null && cellUpdate.getLongitude() != null) {
                    cal.setTimeInMillis(cellUpdate.getTimestamp());
                    pw.printf("%d,%.8f,%.8f,\"%s\"\n", cellUpdate.getId(), cellUpdate.getLongitude(), cellUpdate.getLatitude(), sdf.format(cal.getTime()));
                    pw.flush();
                }
            }
        }
        MatchingResult matchingResult;
        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
            Gson gson = new Gson();
            matchingResult = gson.fromJson(reader, MatchingResult.class);
        }

        Gson gson = new Gson();
        String s = gson.toJson(matchingResult);
        log.info(s);

        for (CellUpdate cellUpdate : cellUPdates) {
            cellUpdate.setDone(true);
        }
        ofy().save().entities(cellUPdates).now();

        for (final DiaryEntry de : matchingResult.diary.entries) {
            ofy().transact(new VoidWork() {
                @Override
                public void vrun() {
                    com.pagr.backend.Route route = new com.pagr.backend.Route();
                    ofy().save().entity(route).now();
                    for (Link link : de.route.links) {
                        LinkPassage linkPassage = new LinkPassage();
                        linkPassage.setWayId(link.id);
                        linkPassage.setSrcNodeId(link.src);
                        linkPassage.setDstNodeId(link.dst);
                        linkPassage.setGeometry(link.geometry);
                        linkPassage.setRouteRef(Ref.create(route));
                        link.linkPassage = ofy().save().entity(linkPassage).now();
                    }
                }
            });
            for (final Link link : de.route.links) {
                List<Key<CellUpdate>> cellUpdates = new ArrayList<>();
                if (link.wpts != null) {
                    for (Wpt wpt : link.wpts) {
                        cellUpdates.add(Key.create(CellUpdate.class, wpt.id));
                    }
                }
                Collection<CellUpdate> values = ofy().load().keys(cellUpdates).values();
                for (final CellUpdate cellUpdate : values) {
                    ofy().transact(new VoidWork() {
                        @Override
                        public void vrun() {
                            ofy().delete().entity(cellUpdate).now();
                            cellUpdate.setMatchedLinkPassage(link.linkPassage);
                            ofy().save().entity(cellUpdate).now();
                        }
                    });
                }
            }
        }
    }

    static class Link {
        long id;
        long src;
        long dst;
        String geometry;
        List<Wpt> wpts;
        transient Key<LinkPassage> linkPassage;
    }

    static class Wpt {
        long id;
    }

    static class Route {
        List<Link> links;
    }

    static class Diary {
        List<DiaryEntry> entries;
    }

    static class DiaryEntry {
        Route route;
    }

    static class MatchingResult {
        Diary diary;
    }

}
