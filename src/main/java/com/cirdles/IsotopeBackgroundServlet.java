package com.cirdles;


import org.cirdles.squid.Squid3API;
import org.cirdles.squid.exceptions.SquidException;
import org.cirdles.squid.shrimp.MassStationDetail;
import org.cirdles.squid.shrimp.SquidSpeciesModel;
import org.cirdles.squid.tasks.Task;
import org.cirdles.squid.tasks.TaskInterface;
import org.cirdles.squid.utilities.stateUtilities.SquidPersistentState;
import org.cirdles.squid.utilities.xmlSerialization.XMLSerializerInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.cirdles.squid.tasks.expressions.builtinExpressions.BuiltInExpressionsDataDictionary.DEFAULT_BACKGROUND_MASS_LABEL;


public class IsotopeBackgroundServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String body[] = request.getReader().lines().collect(Collectors.joining(System.lineSeparator())).split(":");
        Squid3API squid = (Squid3API) this.getServletConfig().getServletContext().getAttribute(body[0]);
        try {
            TaskInterface task = squid.getSquid3Project().getTask();
            if(body[3].equals("copy")) {
                task.applyTaskIsotopeLabelsToMassStationsAndUpdateTask();
            } else {
                List<MassStationDetail> massStationsData = task.makeListOfMassStationDetails();
                        massStationsData.forEach((val) -> {
                    if(body[1].equals(val.getIsotopeAMU())) {
                        if(body[2].equals("true")) {
                            task.setIndexOfBackgroundSpecies(val.getMassStationIndex());
                            task.setIndexOfTaskBackgroundMass(val.getMassStationIndex());
                            val.setIsBackground(true);
                            val.setIsotopeLabel(DEFAULT_BACKGROUND_MASS_LABEL);
                            val.updateTaskIsotopeLabelForBackground(((Task) task).findNominalMassOfTaskBackgroundMass());
                            val.setNumeratorRole(false);
                            val.setDenominatorRole(false);

                            SquidSpeciesModel ssm
                                    = task.getSquidSpeciesModelList()
                                    .get(val.getMassStationIndex());
                            int previousIndex = task.selectBackgroundSpeciesReturnPreviousIndex(ssm);
                            if (previousIndex >= 0) {
                                MassStationDetail previousMassStationDetail = massStationsData.get(previousIndex);
                                previousMassStationDetail.setIsotopeLabel(
                                        task.getSquidSpeciesModelList().get(previousIndex).getIsotopeName());
                                previousMassStationDetail.setTaskIsotopeLabel(task.getNominalMasses().get(previousIndex));
                                previousMassStationDetail.setNumeratorRole(false);
                                previousMassStationDetail.setDenominatorRole(false);

                                SquidSpeciesModel previousSsm = task.getSquidSpeciesModelList().get(previousIndex);
                                previousSsm.setIsBackground(false);
                            }
                            task.setChanged(true);
                        } else {
                            SquidSpeciesModel ssm
                                    = task.getSquidSpeciesModelList()
                                    .get(val.getMassStationIndex());
                            task.setIndexOfBackgroundSpecies(-1);
                            task.setIndexOfTaskBackgroundMass(-1);
                            task.setChanged(true);
                            ssm.setIsBackground(false);
                            val.setIsBackground(false);
                            val.setIsotopeLabel(ssm.getIsotopeName());
                            val.setTaskIsotopeLabel(task.getNominalMasses().get(val.getMassStationIndex()));
                            System.out.println("false");
                        }
                    }
                });
            }
        }
        catch(Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "reportsServlet Servlet";
    }// </editor-fold>
}
