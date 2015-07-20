package com.pagr.odata2;

import com.google.appengine.repackaged.com.google.gson.Gson;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.pagr.backend.CellUpdate;
import com.pagr.backend.LinkPassage;
import com.pagr.backend.Route;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
        List<JSONLineString> jsonLineStrings = new ArrayList<>();
        Gson gson = new Gson();
        if (req.getParameter("cellUpdates") == null || !Boolean.parseBoolean(req.getParameter("cellUpdates"))) {
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
