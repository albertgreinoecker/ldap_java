package at.ac.htlinn.webuntis;

import io.github.cdimascio.dotenv.Dotenv;
import org.bytedream.untis4j.Response;
import org.bytedream.untis4j.Session;
import org.bytedream.untis4j.UntisUtils;
import org.bytedream.untis4j.responseObjects.Classes;
import org.bytedream.untis4j.responseObjects.Rooms;
import org.bytedream.untis4j.responseObjects.Subjects;
import org.bytedream.untis4j.responseObjects.Teachers;
import org.bytedream.untis4j.responseObjects.Timetable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WebUntisWrapper {

    Session session;
    
    public WebUntisWrapper() throws IOException {
        Dotenv dotenv = Dotenv.load();

        String server = dotenv.get("UNTIS_SERVER");
        String school = dotenv.get("UNTIS_SCHOOL");
        String user = dotenv.get("UNTIS_USER");
        String password = dotenv.get("UNTIS_PASSWORD");

        session = Session.login(
                user,
                password,
                server,
                school
        );
    }

    public HashMap<String, Integer> getClassNamesWithId() throws IOException {
        HashMap<String, Integer> nameId = new HashMap<>();
        Classes classes = session.getClasses();

        for (Classes.ClassObject clazz : classes) {
            nameId.put(clazz.getName(), clazz.getId());
        }
        return nameId;
    }

    public Subjects.SubjectObject getSubjectById(int id) throws IOException {
        for (Subjects.SubjectObject subject : session.getSubjects()) {
            if (subject.getId() == id) {
                return subject;
            }
        }
        return null;
    }

    public TeacherInfo getTeacherById(int id) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to   = from.plusMonths(1).minusDays(1);

        HashMap<String, Integer> params = new HashMap<>();
        params.put("id", id);
        params.put("type", 2); // 2 = teacher
        params.put("startDate", Integer.parseInt(from.format(fmt)));
        params.put("endDate",   Integer.parseInt(to.format(fmt)));

        Response response = session.getCustomData("getTimetable", params);
        if (response.isError()) return null;

        JSONArray result = response.getResponse().getJSONArray("result");
        for (int i = 0; i < result.length(); i++) {
            JSONArray teachers = result.getJSONObject(i).optJSONArray("te");
            if (teachers == null) continue;
            for (int j = 0; j < teachers.length(); j++) {
                JSONObject te = teachers.getJSONObject(j);
                if (te.getInt("id") == id) {
                    return new TeacherInfo(id, te.optString("name", ""), te.optString("longname", ""));
                }
            }
        }
        return null;
    }

    // Requires special rights — use getAllTeachersFromTimetable() instead
    public List<Teachers.TeacherObject> getAllTeachers() throws IOException {
        List<Teachers.TeacherObject> list = new ArrayList<>();
        for (Teachers.TeacherObject teacher : session.getTeachers()) {
            list.add(teacher);
        }
        return list;
    }

    public record TeacherInfo(int id, String name, String longName) {}

    /** Extracts unique teachers from a class timetable — no special rights required. */
    public List<TeacherInfo> getAllTeachersFromTimetable(int classId, LocalDate from, LocalDate to) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        HashMap<String, Integer> params = new HashMap<>();
        params.put("id", classId);
        params.put("type", 1);
        params.put("startDate", Integer.parseInt(from.format(fmt)));
        params.put("endDate", Integer.parseInt(to.format(fmt)));

        Response response = session.getCustomData("getTimetable", params);
        if (response.isError()) return List.of();

        Map<Integer, TeacherInfo> byId = new LinkedHashMap<>();
        JSONArray result = response.getResponse().getJSONArray("result");

        for (int i = 0; i < result.length(); i++) {
            JSONArray teachers = result.getJSONObject(i).optJSONArray("te");
            if (teachers == null) continue;
            for (int j = 0; j < teachers.length(); j++) {
                JSONObject te = teachers.getJSONObject(j);
                int id = te.getInt("id");
                byId.putIfAbsent(id, new TeacherInfo(
                        id,
                        te.optString("name", ""),
                        te.optString("longname", "")
                ));
            }
        }
        return new ArrayList<>(byId.values());
    }

    public List<Rooms.RoomObject> getAllRooms() throws IOException {
        List<Rooms.RoomObject> list = new ArrayList<>();
        for (Rooms.RoomObject room : session.getRooms()) {
            list.add(room);
        }
        return list;
    }

    public void dumpTimeTable(int classId, LocalDate day) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dayStr = day.format(fmt);
        HashMap<String,Integer> params = new HashMap<>();
        params.put("id", classId);
        params.put("type", 1); // 1 = class
        params.put("startDate", Integer.parseInt(dayStr));
        params.put("endDate", Integer.parseInt(dayStr));

        Response response = session.getCustomData("getTimetable", params);

        if (response.isError()) {
            System.out.println("Error: " + response.getResponse());
            return;
        }

        JSONArray result = response.getResponse().getJSONArray("result");

        for (int i = 0; i < result.length(); i++) {
            JSONObject period = result.getJSONObject(i);

            System.out.println("=== Lesson " + (i + 1) + " ===");
            System.out.println("Date:    " + period.getInt("date"));
            System.out.println("Start:   " + period.getInt("startTime"));
            System.out.println("End:     " + period.getInt("endTime"));
            System.out.println("Status:  " + period.optString("code", "REGULAR"));

            // Subject
            JSONArray subjects = period.optJSONArray("su");
            if (subjects != null && subjects.length() > 0) {
                System.out.println("Subject: " + subjects.getJSONObject(0).optString("name", "-"));
                int id = Integer.parseInt(subjects.getJSONObject(0).get("id").toString());
                Subjects.SubjectObject sub = getSubjectById( id);
                System.out.println(sub);
            }

            // Room
            JSONArray rooms = period.optJSONArray("ro");
            if (rooms != null && rooms.length() > 0) {
                System.out.println("Room:    " + rooms.getJSONObject(0).optString("name", "-"));
            }
            System.out.println("Teachers:");
            for (TeacherInfo ti : getAllTeachersFromTimetable(classId, day, day))
            {
                System.out.println(ti);

            }
        }
    }

    public void close() throws IOException {
        session.logout();
    }
}
