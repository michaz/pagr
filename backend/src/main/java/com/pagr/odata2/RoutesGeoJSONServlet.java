package com.pagr.odata2;

import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.repackaged.com.google.gson.Gson;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.pagr.backend.CellUpdate;
import com.pagr.backend.LinkPassage;
import com.pagr.backend.Route;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.pagr.backend.OfyService.ofy;

public class RoutesGeoJSONServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(RoutesGeoJSONServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        long id = Long.parseLong(req.getParameter("route"));
        Route route = ofy().load().key(Key.create(Route.class, id)).now();
        List<LinkPassage> list = ofy().load().type(LinkPassage.class).ancestor(route).list();
        route.setLinkPassages(list);
        Gson gson = new Gson();
        if (req.getParameter("cellTowers") != null && Boolean.parseBoolean(req.getParameter("cellTowers"))) {
            Cache cache;
            try {
                CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
                cache = cacheFactory.createCache(Collections.emptyMap());
                Set<String> cellKeys = new HashSet<>();
                JSONCellTowerFeatureCollection jsonCellTowerFeatureCollection = new JSONCellTowerFeatureCollection();
                for (LinkPassage linkPassage : list) {
                    for (Ref<CellUpdate> cellUpdateRef : linkPassage.getCellUpdates()) {
                        CellKey cellKey = new CellKey();
                        cellKey.mcc = cellUpdateRef.get().getMcc();
                        cellKey.mnc = cellUpdateRef.get().getMnc();
                        cellKey.lac = cellUpdateRef.get().getTac();
                        cellKey.cellid = cellUpdateRef.get().getCi();
                        cellKeys.add(gson.toJson(cellKey));
                    }
                }
                Map<String, String> fromCache = cache.getAll(cellKeys);
                for (String cellKeyJSON : cellKeys) {
                    JSONCellTowerFeature feature = gson.fromJson(fromCache.get(cellKeyJSON), JSONCellTowerFeature.class);
                    if (feature != null && feature.properties.code == 0) {
                        jsonCellTowerFeatureCollection.features.add(feature);
                        log.info("Have: "+cellKeyJSON);
                    } else if (feature == null) {
                        Queue queue = QueueFactory.getDefaultQueue();
                        queue.add(TaskOptions.Builder.withPayload(new FetchTask(cellKeyJSON)));
                        log.info("Don't have (will fetch): "+cellKeyJSON);
                    } else {
                        // We have a cached error message (probably cell doesn't exist on OpenCellID)
                        log.info("Permanently don't have: "+cellKeyJSON);
                    }
                }
                try (OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream())) {
                    gson.toJson(jsonCellTowerFeatureCollection, osw);
                }
            } catch (CacheException e) {
                throw new RuntimeException(e);
            }
        } else if (req.getParameter("cellUpdates") == null || !Boolean.parseBoolean(req.getParameter("cellUpdates"))) {
            List<JSONLineString> jsonLineStrings = new ArrayList<>();
            JSONLinkPassageFeatureCollection jsonFeatureCollection = new JSONLinkPassageFeatureCollection();
            for (LinkPassage linkPassage : list) {
                JSONLineString jsonLineString = gson.fromJson(linkPassage.getGeometry(), JSONLineString.class);
                jsonLineStrings.add(jsonLineString);
                JSONLinkPassageFeature feature = new JSONLinkPassageFeature();
                feature.geometry = jsonLineString;
                feature.properties = linkPassage;
                jsonFeatureCollection.features.add(feature);
            }
            JSONGeometryCollection jsonGeometryCollection = new JSONGeometryCollection();
            jsonGeometryCollection.geometries = jsonLineStrings;
            try (OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream())) {
                gson.toJson(jsonFeatureCollection, osw);
            }
        } else {
            JSONCellUpdateFeatureCollection jsonFeatureCollection = new JSONCellUpdateFeatureCollection();
            for (LinkPassage linkPassage : list) {
                for (Ref<CellUpdate> cellUpdateRef : linkPassage.getCellUpdates()) {
                    JSONCellUpdateFeature feature = new JSONCellUpdateFeature();
                    feature.geometry = new JSONPoint();
                    feature.geometry.coordinates = new double[]{cellUpdateRef.get().getLongitude(),cellUpdateRef.get().getLatitude()};
                    feature.properties = cellUpdateRef.get();
                    jsonFeatureCollection.features.add(feature);
                }
            }
            try (OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream())) {
                gson.toJson(jsonFeatureCollection, osw);
            }
        }
    }

    private static class FetchTask implements DeferredTask {
        private final String cellKeyJSON;

        public FetchTask(String cellKey) {
            this.cellKeyJSON = cellKey;
        }

        @Override
        public void run() {
            Gson gson = new Gson();
            try {
                CellKey cellKey = gson.fromJson(cellKeyJSON, CellKey.class);
                String queryString = "mcc="+ cellKey.mcc+"&mnc="+ cellKey.mnc+"&lac="+ cellKey.lac+"&cellid="+ cellKey.cellid;
                URL url = new URL("http://opencellid.org/cell/get?key=9cdca40d-d27f-4917-8d7d-9bb361416217&format=json&" + queryString);
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(600000);
                connection.setReadTimeout(600000);
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JSONCellTower result = gson.fromJson(reader, JSONCellTower.class);
                    if (result.code != 0) {
                        log.info("Error from openCellId: "+result.code+" "+result.error);
                    }
                    JSONCellTowerFeature feature = new JSONCellTowerFeature();
                    feature.properties = result;
                    feature.geometry = new JSONPoint();
                    feature.geometry.coordinates = new double[]{result.lon,result.lat};
                    Cache cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
                    String value = gson.toJson(feature);
                    log.info("Caching: "+ cellKeyJSON + " ------> " + value);
                    cache.put(cellKeyJSON, value);
                }
            } catch (IOException | CacheException e) {
                throw new RuntimeException(e);
            }
        }
    }


    static class CellKey {
        int mcc;
        int mnc;
        int lac;
        int cellid;
    }

    static class JSONCellTowerFeatureCollection {
        String type = "FeatureCollection";
        List<JSONCellTowerFeature> features = new ArrayList<>();
    }

    static class JSONCellTowerFeature {
        String type = "Feature";
        JSONPoint geometry;
        JSONCellTower properties;
    }

    static class JSONCellTower {
        Double lon;
        Double lat;
        int mcc;
        int mnc;
        int lac;
        int cellid;
        int averageSignalStrength;
        int range;
        int samples;
        boolean changeable;
        String radio;
        int sid;
        int nid;
        int bid;
        // The same class is used for positive and negative responses...
        int code;
        String error;
    }

    static class JSONLinkPassageFeatureCollection {
        String type = "FeatureCollection";
        List<JSONLinkPassageFeature> features = new ArrayList<>();
    }

    static class JSONCellUpdateFeatureCollection {
        String type = "FeatureCollection";
        List<JSONCellUpdateFeature> features = new ArrayList<>();
    }


    static class JSONLinkPassageFeature {
        String type = "Feature";
        JSONLineString geometry;
        LinkPassage properties;
    }

    static class JSONCellUpdateFeature {
        String type = "Feature";
        JSONPoint geometry;
        CellUpdate properties;
    }

    static class JSONPoint {
        String type = "Point";
        double[] coordinates;
    }

    static class JSONGeometryCollection {
        String type = "GeometryCollection";
        List<JSONLineString> geometries;
    }
    static class JSONLineString {
        String type = "LineString";
        List<double[]> coordinates;
    }

}
