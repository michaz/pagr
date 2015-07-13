package com.pagr.odata2;

import com.google.appengine.repackaged.com.google.gson.Gson;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.VoidWork;
import com.pagr.backend.CellUpdate;
import com.pagr.backend.LinkPassage;
import com.pagr.backend.Route;

import java.io.IOException;
import java.io.InputStreamReader;
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
        for (LinkPassage linkPassage : list) {
            JSONLineString jsonLineString = gson.fromJson(linkPassage.getGeometry(), JSONLineString.class);
            jsonLineStrings.add(jsonLineString);
        }
        JSONGeometryCollection jsonGeometryCollection = new JSONGeometryCollection();
        jsonGeometryCollection.geometries = jsonLineStrings;
        try (OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream())) {
            gson.toJson(jsonGeometryCollection, osw);
        }
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
