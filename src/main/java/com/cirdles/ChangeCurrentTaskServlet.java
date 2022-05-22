package com.cirdles;


import org.cirdles.squid.Squid3API;
import org.cirdles.squid.constants.Squid3Constants;
import org.cirdles.squid.core.CalamariReportsEngine;
import org.cirdles.squid.core.PrawnXMLFileHandler;
import org.cirdles.squid.exceptions.SquidException;
import org.cirdles.squid.projects.Squid3ProjectBasicAPI;
import org.cirdles.squid.shrimp.MassStationDetail;
import org.cirdles.squid.shrimp.ShrimpDataFileInterface;
import org.cirdles.squid.tasks.Task;
import org.cirdles.squid.tasks.TaskInterface;
import org.cirdles.squid.tasks.taskDesign.TaskDesign;
import org.cirdles.squid.utilities.fileUtilities.FileValidator;
import org.cirdles.squid.utilities.stateUtilities.SquidPersistentState;
import org.cirdles.squid.utilities.xmlSerialization.XMLSerializerInterface;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.cirdles.squid.constants.Squid3Constants.SQUID_TASK_LIBRARY_FOLDER;
import static org.cirdles.squid.constants.Squid3Constants.XML_HEADER_FOR_SQUIDTASK_FILES_USING_LOCAL_SCHEMA;
import static org.cirdles.squid.utilities.fileUtilities.FileValidator.validateXML;


public class ChangeCurrentTaskServlet extends HttpServlet {
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
            Squid3ProjectBasicAPI squidProject = squid.getSquid3Project();
            File taskFile = body[1].equals("default") ? SQUID_TASK_LIBRARY_FOLDER
                    : new File(Constants.TOMCAT_ROUTE + File.separator + "filebrowser" + File.separator + "users" + File.separator + body[0] + File.separator + body[1]);
            ArrayList<TaskInterface> taskList = populateListOfTasks(squidProject, taskFile);
            TaskInterface chosenTask;
            for(TaskInterface listItem : taskList) {
                if(listItem.getName().equals(body[2])) {
                    chosenTask = listItem;
                    TaskDesign taskEditor = SquidPersistentState.getExistingPersistentState().getTaskDesign();

                    if (squidProject.getTask().getTaskType().equals(chosenTask.getTaskType())) {
                        // check the mass count
                        boolean valid = (squidProject.getTask().getSquidSpeciesModelList().size()
                                == (chosenTask.getNominalMasses().size()));
                        if (valid) {
                            squidProject.getPrawnFileHandler();
                            // due to associating commonPb and Physical Constants with Project, need to update design
                            // this issue was overlooked and noticed in issue #655
                            chosenTask.setCommonPbModel(squidProject.getCommonPbModel());
                            chosenTask.setPhysicalConstantsModel(squidProject.getPhysicalConstantsModel());

                            chosenTask.updateTaskDesignFromTask(taskEditor, true);
                            try {
                                squidProject.createNewTask();
                                squidProject.getTask().updateTaskFromTaskDesign(taskEditor, false);
                            } catch (SquidException squidException) {
                                System.out.println(squidException.getMessage());
                            }

                        }
                }
            }
        }}
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
    public static ArrayList<TaskInterface> populateListOfTasks(Squid3ProjectBasicAPI squidProject, File route) {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema taskXMLSchema = null;
        try {
            taskXMLSchema = sf.newSchema(new File(Squid3Constants.URL_STRING_FOR_SQUIDTASK_XML_SCHEMA_LOCAL));
        } catch (SAXException e) {
            e.printStackTrace();
        }

        ArrayList<TaskInterface> taskFilesInFolder = new ArrayList<>();
        File tasksBrowserTarget = route;
        String tasksBrowserType = ".xml";
        if (tasksBrowserTarget != null) {
            if (tasksBrowserType.compareToIgnoreCase(".xml") == 0) {
                if (tasksBrowserTarget.isDirectory()) {
                    // collect Tasks if any
                    for (File file : tasksBrowserTarget.listFiles(new FilenameFilter() {
                                                                      @Override
                                                                      public boolean accept(File file, String name) {
                                                                          return name.toLowerCase().endsWith(".xml");
                                                                      }
                                                                  }
                    )) {
                        // check if task
                        try {
                            TaskInterface task = (Task) ((XMLSerializerInterface)  // Filtering out non-Task XML files
                                    squidProject.getTask()).readXMLObject(file.getAbsolutePath(), false);
                            if (task != null) {
                                FileValidator.validateXML(file, taskXMLSchema, XML_HEADER_FOR_SQUIDTASK_FILES_USING_LOCAL_SCHEMA);
                                taskFilesInFolder.add(task); // Not added if exception thrown from validateXML
                            }
                        } catch (IOException | ArrayIndexOutOfBoundsException | SAXException e) {
                        }
                    } // End for loop for files
                } else {
                    // check if task
                    try {
                        TaskInterface task = (Task) ((XMLSerializerInterface)  // Filtering out non-Task XML files
                                squidProject.getTask()).readXMLObject(tasksBrowserTarget.getAbsolutePath(), false);
                        if (task != null) {
                            validateXML(tasksBrowserTarget, taskXMLSchema, XML_HEADER_FOR_SQUIDTASK_FILES_USING_LOCAL_SCHEMA);
                            taskFilesInFolder.add(task); // Not added if exception thrown from validateXML
                        }
                    } catch (IOException | ArrayIndexOutOfBoundsException | SAXException e) {
                    }
                }
            }
        }
        return taskFilesInFolder;

    }
    //public void createNewTask() throws SquidException {
    //    ShrimpDataFileInterface prawnFile = null;
    //    CalamariReportsEngine prawnFileHandler = new PrawnXMLFileHandler();
    //    Task task = new Task("New Task", prawnFile, prawnFileHandler.getNewReportsEngine());
    //}
}
