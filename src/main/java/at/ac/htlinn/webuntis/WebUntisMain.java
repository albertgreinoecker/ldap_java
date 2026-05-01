package at.ac.htlinn.webuntis;

import org.bytedream.untis4j.responseObjects.Rooms;
import org.bytedream.untis4j.responseObjects.Teachers;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class WebUntisMain {
    public static <Klassen> void main(String[] args) throws IOException {


        WebUntisWrapper untis = new WebUntisWrapper();
        HashMap<String, Integer> nameId = untis.getClassNamesWithId();
        for (Map.Entry<String,Integer> ni : nameId.entrySet())
        {
            System.out.printf("%s:%d %n", ni.getKey(), ni.getValue());
        }
        LocalDate day = LocalDate.now().minusDays(1);
        untis.dumpTimeTable(6159, day);


        for(Rooms.RoomObject ro : untis.getAllRooms())
        {
            System.out.println(ro);
        }
        untis.close();
    }
}
