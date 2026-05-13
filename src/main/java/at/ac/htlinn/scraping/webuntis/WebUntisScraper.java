package at.ac.htlinn.scraping.webuntis;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;

public class WebUntisScraper implements AutoCloseable {

    private final Playwright playwright;
    private final Browser browser;
    private final Page page;
    private final String baseUrl;

    public record Lesson(String day, String klasse, String subject, String room, boolean cancelled) {}

    public WebUntisScraper() {
        Dotenv dotenv = Dotenv.load();
        String server   = dotenv.get("UNTIS_SERVER");
        String school   = dotenv.get("UNTIS_SCHOOL");
        String user     = dotenv.get("UNTIS_USER");
        String password = dotenv.get("UNTIS_PASSWORD");

        baseUrl = server + "/WebUntis";

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(false)
        );
        page = browser.newPage();
        login(school, user, password);
    }

    private void login(String school, String user, String password) {
        page.navigate(baseUrl + "/?school=" + school);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.fill("input.un-input-group__input[type='text']", user);
        page.fill("input.un-input-group__input[type='password']", password);
        page.click("button[type='submit']");
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public List<Lesson> scrapeMyTimetable() {
        page.click("a[href='/timetable/my-teacher']");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        List<Lesson> lessons = new ArrayList<>();

        for (ElementHandle column : page.querySelectorAll(".timetable-grid--column-container")) {
            String day = textOf(column, ".column-header-print-label-text");

            for (ElementHandle card : column.querySelectorAll(".timetable-grid-card")) {
                ElementHandle lessonCard = card.querySelector(".lesson-card");
                if (lessonCard == null) continue;

                String classes = lessonCard.getAttribute("class");
                boolean cancelled = classes != null && classes.contains("cancelled");

                String klasse  = textOf(card, "[data-testid='lesson-card-resources-with-change-klasse'] [data-testid='regular-resource']");
                String subject = textOf(card, "[data-testid='lesson-card-resources-with-change-subject'] [data-testid='regular-resource']");
                String room    = textOf(card, "[data-testid='lesson-card-resources-with-change-rooms'] [data-testid='regular-resource']");

                lessons.add(new Lesson(day, klasse, subject, room, cancelled));
            }
        }

        return lessons;
    }

    public String dumpHtml() {
        page.click("a[href='/timetable/my-teacher']");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        return page.content();
    }

    private String textOf(ElementHandle parent, String selector) {
        ElementHandle child = parent.querySelector(selector);
        return child != null ? child.innerText().strip() : "";
    }

    @Override
    public void close() {
        browser.close();
        playwright.close();
    }
}
