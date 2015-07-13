package com.pagr.odata2;

import com.pagr.backend.CellUpdate;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.pagr.backend.OfyService.ofy;

public class CellUpdatesCSVServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/csv");
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try (PrintWriter pw = new PrintWriter(response.getOutputStream())) {
            for (CellUpdate cellUpdate : ofy().load().type(CellUpdate.class).order("timestamp").iterable()) {
                if (cellUpdate.getLatitude() != null && cellUpdate.getLongitude() != null) {
                    cal.setTimeInMillis(cellUpdate.getTimestamp());
                    pw.printf("%d,%.8f,%.8f,\"%s\"\n", cellUpdate.getId(), cellUpdate.getLongitude(), cellUpdate.getLatitude(), sdf.format(cal.getTime()));
                    pw.flush();
                }
            }
        }
    }
}
