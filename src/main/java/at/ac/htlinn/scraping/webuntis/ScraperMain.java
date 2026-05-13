package at.ac.htlinn.scraping.webuntis;

public class ScraperMain {
    public static void main(String[] args) throws Exception {
        try (WebUntisScraper scraper = new WebUntisScraper()) {
            for (WebUntisScraper.Lesson lesson : scraper.scrapeMyTimetable()) {
                System.out.println(
                    lesson.day() + " | " +
                    lesson.klasse() + " | " +
                    lesson.subject() + " | " +
                    lesson.room() +
                    (lesson.cancelled() ? " | ENTFALLEN" : "")
                );
            }
        }
    }
}
