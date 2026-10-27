import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MovieWebServer {

    private static final int PORT = 8080;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private static final List<MovieData> movies = new ArrayList<>();

    // In-memory storage: no database required.
    private static final Set<String> bookedSeats =
            ConcurrentHashMap.newKeySet();

    private static final List<String> bookingHistory =
            new CopyOnWriteArrayList<>();

    private static final String[] ALL_SEATS = {
            "A1", "A2", "A3", "A4", "A5", "A6",
            "B1", "B2", "B3", "B4", "B5", "B6",
            "C1", "C2", "C3", "C4", "C5", "C6",
            "D1", "D2", "D3", "D4", "D5", "D6",
            "E1", "E2", "E3", "E4", "E5", "E6"
    };

    static class MovieData {
        String title;
        String language;
        String releaseDate;
        String description;

        MovieData(String title, String language,
                  String releaseDate, String description) {
            this.title = title;
            this.language = language;
            this.releaseDate = releaseDate;
            this.description = description;
        }
    }

    public static void main(String[] args) throws IOException {
        loadMovies();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0);

        server.createContext("/", MovieWebServer::handleRequest);
        server.setExecutor(null);
        server.start();

        System.out.println("====================================");
        System.out.println(" MOVIE TICKET BOOKING SYSTEM ACTIVE");
        System.out.println(" Open: http://localhost:" + PORT + "/");
        System.out.println("====================================");
    }

    private static void loadMovies() {
        // Existing catalogue
        add("Godzilla Minus Zero", "English", "2026-11-05",
                "A giant-monster cinematic adventure.");
        add("The Hunger Games: Sunrise on the Reaping",
                "English", "2026-11-20",
                "A new chapter in the Hunger Games universe.");
        add("Aata Nade Veta Nade", "Telugu", "2027-12-27",
                "Upcoming Telugu movie.");
        add("Vidudala", "Telugu", "2027-01-12",
                "Upcoming Telugu movie.");
        add("Aakasam Lo Oka Tara", "Telugu", "2026-11-20",
                "Upcoming Telugu movie.");
        add("Avengers: Doomsday", "English", "2026-12-18",
                "A superhero adventure.");
        add("Dune: Part Three", "English", "2026-12-18",
                "A science-fiction adventure.");
        add("Jumanji: Open World", "English", "2026-12-25",
                "An adventure in the Jumanji universe.");
        add("Pushpa 2: The Rule", "Telugu", "2026-12-01",
                "Listed as an upcoming title in this project.");
        add("Vrushakarma", "Telugu", "2026-12-04",
                "Upcoming Telugu movie.");
        add("Mookuthi Amman 2", "Tamil", "2026-11-01",
                "Upcoming Tamil movie.");
        add("Indian 3", "Tamil", "2026-10-01",
                "Upcoming Tamil movie.");
        add("Ramayana: Part One", "Hindi", "2026-11-01",
                "An epic mythological adventure.");
        add("Demon Slayer: Infinity Castle", "Anime", "",
                "Upcoming anime feature.");
        add("Spider-Man: Beyond the Spider-Verse",
                "English", "2027-06-16",
                "An animated superhero adventure.");
        add("Shrek 5", "English", "2027-06-29",
                "An animated comedy adventure.");
        add("Avengers: Secret Wars", "English", "2027-12-01",
                "Upcoming superhero movie.");
        add("The Batman: Part II", "English", "2028-01-01",
                "Upcoming superhero movie.");
        add("Varanasi", "Telugu", "2027-01-01",
                "Upcoming Telugu movie.");
        add("Spirit", "Telugu", "2027-01-01",
                "Upcoming Telugu movie.");
        add("Dragon", "Telugu", "2027-01-01",
                "Upcoming Telugu movie.");
        add("Goodachari 2 (G2)", "Telugu", "2027-01-01",
                "Upcoming Telugu spy thriller.");
        add("Kalki 2898 AD Sequel", "Telugu", "2028-01-01",
                "Upcoming science-fiction sequel.");

        // October 2026 releases supplied for this project
        add("B.A / Brindavihari / Drones", "English / Kannada / Tamil",
                "2026-10-16", "Listed October cinema release.");
        add("Nayyi Navelli", "Regional",
                "2026-10-16", "Listed October cinema release.");
        add("Tum Mere Ho", "Hindi",
                "2026-10-23", "Romantic drama.");
        add("Magazine", "Marathi / English",
                "2026-10-23", "Listed October cinema release.");
        add("BTS Arirang in Sao Paulo: Live Viewing", "Korean",
                "2026-10-25", "Concert live-viewing event.");
        add("Sense and Sensibility", "English / Hindi",
                "2026-10-30", "Listed cinema release.");
        add("Other Mommy", "English",
                "2026-10-31", "Listed cinema release.");
        add("The Social Reckoning", "English",
                "2026-10-31", "Listed cinema release.");
        add("Avengers: Endgame Encore", "English / Hindi",
                "2026-10-31", "Special screening.");
    }

    private static void add(String title, String language,
                            String releaseDate, String description) {
        movies.add(new MovieData(title, language, releaseDate, description));
    }

    private static void handleRequest(HttpExchange exchange)
            throws IOException {
        String path = exchange.getRequestURI().getPath();
        Map<String, List<String>> params = readParams(exchange);

        String movieTitle = first(params, "movie", "");
        String date = first(params, "date", "");
        String show = first(params, "show", "Evening");
        MovieData movie = findMovie(movieTitle);

        String html;

        try {
            switch (path) {
                case "/":
                case "/home":
                    html = homePage();
                    break;

                case "/movies":
                    html = moviesPage();
                    break;

                case "/movie":
                    html = movie == null
                            ? errorPage("Movie not found.")
                            : moviePage(movie);
                    break;

                case "/theatre":
                    if (movie == null) {
                        html = errorPage("Movie not found.");
                    } else if (!isBookableMovie(movie)) {
                        html = comingSoonPage(movie);
                    } else {
                        html = theatrePage(movie);
                    }
                    break;

                case "/seats":
                    if (!validBooking(movie, date)) {
                        html = comingSoonPage(movie);
                    } else {
                        html = seatsPage(movie, date, show);
                    }
                    break;

                case "/payment":
                    if (!validBooking(movie, date)) {
                        html = comingSoonPage(movie);
                    } else {
                        List<String> seats = params.getOrDefault(
                                "seats", Collections.emptyList());
                        html = paymentPage(movie, date, show, seats);
                    }
                    break;

                case "/confirm":
                    if (!validBooking(movie, date)) {
                        html = comingSoonPage(movie);
                    } else {
                        List<String> seats = params.getOrDefault(
                                "seats", Collections.emptyList());
                        html = confirmBooking(movie, date, show, seats);
                    }
                    break;

                case "/history":
                    html = historyPage();
                    break;

                case "/contact":
                    html = contactPage();
                    break;

                default:
                    html = errorPage("Page not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            html = errorPage("Something went wrong. Please try again.");
        }

        sendHtml(exchange, html);
    }

    private static Map<String, List<String>> readParams(
            HttpExchange exchange) throws IOException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);
            raw = raw == null || raw.isEmpty() ? body : raw + "&" + body;
        }

        if (raw == null || raw.isEmpty()) {
            return result;
        }

        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;

            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";

            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return result;
    }

    private static String first(Map<String, List<String>> params,
                                String key, String fallback) {
        List<String> values = params.get(key);
        return values == null || values.isEmpty() ? fallback : values.get(0);
    }

    private static MovieData findMovie(String title) {
        for (MovieData movie : movies) {
            if (movie.title.equals(title)) return movie;
        }
        return null;
    }

    /*
     * Only movies with exact dates in 2026 can be booked in this version.
     * Month-only, year-only and 2027/2028 releases remain Coming Soon.
     */
   
private static boolean isBookableMovie(MovieData movie) {
    if (movie == null || movie.releaseDate == null
            || movie.releaseDate.isEmpty()) {
        return false;
    }

    try {
        LocalDate release = LocalDate.parse(
                movie.releaseDate, DATE_FORMAT);

        return release.getYear() == 2026
                && release.getMonthValue() >= 10
                && release.getMonthValue() <= 12;
    } catch (DateTimeParseException e) {
        return false;
    }
}


    private static LocalDate firstBookingDate(MovieData movie) {
        LocalDate release = LocalDate.parse(movie.releaseDate, DATE_FORMAT);
        LocalDate today = LocalDate.now();
        return release.isBefore(today) ? today : release;
    }

private static boolean validBooking(MovieData movie, String date) {
    if (!isBookableMovie(movie) || date == null || date.isEmpty()) {
        return false;
    }

    try {
        LocalDate selectedDate = LocalDate.parse(date, DATE_FORMAT);
        LocalDate releaseDate = LocalDate.parse(
                movie.releaseDate, DATE_FORMAT);

        LocalDate firstDate = releaseDate.isBefore(LocalDate.now())
                ? LocalDate.now()
                : releaseDate;

        return !selectedDate.isBefore(firstDate);
    } catch (DateTimeParseException e) {
        return false;
    }
}


   
private static String homePage() {
    StringBuilder b = new StringBuilder(pageStart("Home"));

    b.append("<section class='hero'>")
     .append("<p class='eyebrow'>YOUR NEXT CINEMA EXPERIENCE</p>")
     .append("<h1>Movies. Moments. Memories.</h1>")
     .append("<p>Book 2026 releases and discover upcoming 2027 movies.</p>")
     .append("<a class='btn' href='/movies'>Explore All Movies</a>")
     .append("</section>");

    b.append("<h2>🎬 October - December 2026 | Book Tickets</h2>")
     .append("<div class='grid'>");

    for (MovieData m : movies) {
        if (isBookableMovie(m)) {
            b.append(movieCard(m));
        }
    }

    b.append("</div>");

    b.append("<h2>🔮 Upcoming Movies - 2027</h2>")
     .append("<p>These movies are listed for 2027. Booking is not available yet.</p>")
     .append("<div class='grid'>");

    for (MovieData m : movies) {
        if (m.releaseDate != null
                && m.releaseDate.matches("2027-\\d{2}-\\d{2}")) {
            b.append(movieCard(m));
        }
    }

    b.append("</div>");

    b.append("<h2>✨ More Upcoming Movies</h2>")
     .append("<div class='grid'>");

    for (MovieData m : movies) {
        if (m.releaseDate == null
                || m.releaseDate.isEmpty()
                || m.releaseDate.startsWith("2028")
                || !m.releaseDate.matches("\\d{4}-\\d{2}-\\d{2}")
                && !m.releaseDate.startsWith("2027")) {
            b.append(movieCard(m));
        }
    }

    b.append("</div>");

    return b.append(pageEnd()).toString();
}


private static String moviesPage() {
    StringBuilder b = new StringBuilder(pageStart("Movies"));

    b.append("<h1>Movie Catalogue</h1>")
    .append("<h2>🎟️ October - December 2026 | Book Tickets</h2>")
    .append("<div class='grid'>");

    for (MovieData m : movies) {
        if (isBookableMovie(m)) {
            b.append(movieCard(m));
        }
    }

    b.append("</div><h2>🔮 Upcoming Movies - 2027</h2>")
     .append("<div class='grid'>");

    for (MovieData m : movies) {
        if (m.releaseDate != null
                && m.releaseDate.startsWith("2027-")) {
            b.append(movieCard(m));
        }
    }

    b.append("</div><h2>✨ Other Upcoming Movies</h2>")
     .append("<div class='grid'>");

    for (MovieData m : movies) {
        if (m.releaseDate == null
                || m.releaseDate.isEmpty()
                || m.releaseDate.startsWith("2028-")) {
            b.append(movieCard(m));
        }
    }

    b.append("</div>");
    return b.append(pageEnd()).toString();
}

    
    private static String moviePage(MovieData m) {
        StringBuilder b = new StringBuilder(pageStart(m.title));
        b.append("<section class='panel'><span class='tag'>")
         .append(esc(m.language)).append("</span><h1>")
         .append(esc(m.title)).append("</h1><p>")
         .append(esc(m.description)).append("</p><p><b>Release date:</b> ")
         .append(releaseDisplay(m)).append("</p>");

        if (isBookableMovie(m)) {
            b.append("<a class='btn' href='/theatre?movie=")
             .append(enc(m.title)).append("'>Choose Theatre and Show</a>");
        } else {
            b.append("<p class='warning'>Booking unavailable — Coming Soon.</p>");
        }

        b.append("</section>");
        return b.append(pageEnd()).toString();
    }


    
private static String movieCard(MovieData m) {
    StringBuilder b = new StringBuilder();

    b.append("<div class='card'>")
     .append("<span class='tag'>")
     .append(esc(m.language))
     .append("</span>")
     .append("<h3>")
     .append(esc(m.title))
     .append("</h3>")
     .append("<p>")
     .append(esc(m.description))
     .append("</p>")
     .append("<p><b>Release date:</b> ")
     .append(esc(releaseDisplay(m)))
     .append("</p>");

    if (isBookableMovie(m)) {
        b.append("<a class='btn' href='/movie?movie=")
         .append(enc(m.title))
         .append("'>View Movie</a>");
    } else {
        b.append("<a class='btn secondary' href='/movie?movie=")
         .append(enc(m.title))
         .append("'>Coming Soon</a>");
    }

    b.append("</div>");
    return b.toString();
}


    private static String theatrePage(MovieData m) {
        LocalDate start = firstBookingDate(m);
        StringBuilder b = new StringBuilder(pageStart("Choose Show"));

        b.append("<h1>Choose Theatre and Show</h1><div class='panel'>")
         .append("<h2>").append(esc(m.title)).append("</h2>")
         .append("<p>Demo theatre: SRM Cinemas</p>")
         .append("<form action='/seats' method='get'>")
         .append("<input type='hidden' name='movie' value='")
         .append(attr(m.title)).append("'>")
         .append("<label>Show date</label><select name='date' required>");

        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            b.append("<option value='").append(d.format(DATE_FORMAT))
             .append("'>").append(d.format(DISPLAY_DATE))
             .append("</option>");
        }

        b.append("</select><label>Showtime</label><select name='show'>")
         .append("<option>Morning</option><option>Afternoon</option>")
         .append("<option>Evening</option><option>Night</option>")
         .append("</select><button class='btn' type='submit'>Choose Seats</button>")
         .append("</form></div>");

        return b.append(pageEnd()).toString();
    }

    private static String seatsPage(MovieData m, String date, String show) {
        StringBuilder b = new StringBuilder(pageStart("Select Seats"));

        b.append("<h1>Select Your Seats</h1><div class='panel'>")
         .append("<p><b>").append(esc(m.title)).append("</b></p>")
         .append("<p>").append(esc(displayDate(date)))
         .append(" · ").append(esc(show)).append("</p>")
         .append("<div class='screen'>SCREEN</div>")
         .append("<form action='/payment' method='post'>")
         .append(hidden("movie", m.title))
         .append(hidden("date", date))
         .append(hidden("show", show))
         .append("<div class='seat-grid'>");

        for (String seat : ALL_SEATS) {
            String key = seatKey(m.title, date, show, seat);
            boolean occupied = bookedSeats.contains(key);

            b.append("<label class='seat ")
             .append(occupied ? "occupied" : "")
             .append("'><input type='checkbox' name='seats' value='")
             .append(seat).append("' ")
             .append(occupied ? "disabled" : "")
             .append("><span>").append(seat).append("</span></label>");
        }

        b.append("</div><p class='muted'>Choose one or more available seats.</p>")
         .append("<button class='btn' type='submit'>Continue to Checkout</button>")
         .append("</form></div>");

        return b.append(pageEnd()).toString();
    }

    private static String paymentPage(MovieData m, String date, String show,
                                      List<String> selectedSeats) {
        List<String> seats = cleanSeats(selectedSeats);

        if (seats.isEmpty()) {
            return pageStart("Select Seats")
                    + "<h1>Please select at least one seat.</h1>"
                    + "<a class='btn' href='/theatre?movie=" + enc(m.title)
                    + "'>Go Back</a>" + pageEnd();
        }

        for (String seat : seats) {
            if (bookedSeats.contains(seatKey(m.title, date, show, seat))) {
                return errorPage("A selected seat is no longer available. Please select again.");
            }
        }

        int total = seats.size() * 180;
        StringBuilder b = new StringBuilder(pageStart("Checkout"));

        b.append("<h1>Booking Checkout</h1><div class='panel'>")
         .append("<p><b>Movie:</b> ").append(esc(m.title)).append("</p>")
         .append("<p><b>Date:</b> ").append(esc(displayDate(date))).append("</p>")
         .append("<p><b>Show:</b> ").append(esc(show)).append("</p>")
         .append("<p><b>Seats:</b> ").append(esc(String.join(", ", seats)))
         .append("</p><h2>Total: ₹").append(total).append("</h2>")
         .append("<p class='muted'>Demo checkout — no real payment is collected.</p>")
         .append("<form action='/confirm' method='post'>")
         .append(hidden("movie", m.title))
         .append(hidden("date", date))
         .append(hidden("show", show));

        for (String seat : seats) {
            b.append(hidden("seats", seat));
        }

        b.append("<button class='btn' type='submit'>Confirm Booking</button>")
         .append("</form></div>");

        return b.append(pageEnd()).toString();
    }

    private static String confirmBooking(MovieData m, String date, String show,
                                         List<String> selectedSeats) {
        List<String> seats = cleanSeats(selectedSeats);

        if (seats.isEmpty()) {
            return errorPage("No seats were selected.");
        }

        // Check and reserve seats together to reduce double-booking risk.
        synchronized (bookedSeats) {
            for (String seat : seats) {
                if (bookedSeats.contains(seatKey(m.title, date, show, seat))) {
                    return errorPage("A seat was just booked by someone else. Please select again.");
                }
            }

            for (String seat : seats) {
                bookedSeats.add(seatKey(m.title, date, show, seat));
            }
        }

        String bookingId = "MTB" + System.currentTimeMillis();
        String details = bookingId + " | " + m.title + " | "
                + displayDate(date) + " | " + show + " | "
                + String.join(", ", seats);

        bookingHistory.add(details);

        return pageStart("Booking Confirmed")
                + "<section class='panel success'><h1>Booking Confirmed!</h1>"
                + "<p>Your demo booking has been recorded.</p><p><b>Booking ID:</b> "
                + esc(bookingId) + "</p><p><b>Movie:</b> " + esc(m.title)
                + "</p><p><b>Date:</b> " + esc(displayDate(date))
                + "</p><p><b>Show:</b> " + esc(show)
                + "</p><p><b>Seats:</b> " + esc(String.join(", ", seats))
                + "</p><p><b>Total:</b> ₹" + (seats.size() * 180)
                + "</p><a class='btn' href='/movies'>Browse More Movies</a> "
                + "<a class='btn secondary' href='/history'>Booking History</a>"
                + "</section>" + pageEnd();
    }

    private static String historyPage() {
        StringBuilder b = new StringBuilder(pageStart("Booking History"));
        b.append("<h1>Booking History</h1>");

        if (bookingHistory.isEmpty()) {
            b.append("<div class='panel'><p>No bookings yet in this server session.</p></div>");
        } else {
            b.append("<div class='panel'><ul>");
            for (String booking : bookingHistory) {
                b.append("<li>").append(esc(booking)).append("</li>");
            }
            b.append("</ul></div>");
        }

        return b.append(pageEnd()).toString();
    }

    private static String contactPage() {
        return pageStart("Contact")
                + "<h1>Contact Us</h1><div class='panel'>"
                + "<p><b>Movie Ticket Booking System</b></p>"
                + "<p>Student project demonstration website.</p>"
                + "<p>Email: support@example.com</p>"
                + "<p class='muted'>Demo contact details — replace with your project details.</p>"
                + "</div>" + pageEnd();
    }

    private static String comingSoonPage(MovieData m) {
        String title = m == null ? "Movie" : m.title;

        return pageStart("Coming Soon")
                + "<section class='panel'><h1>🎬 COMING SOON</h1>"
                + "<h2>" + esc(title) + "</h2>"
                + "<p>Booking is unavailable for this movie or selected date.</p>"
                + "<p>Booking opens on the eligible release date.</p>"
                + "<a class='btn' href='/movies'>Browse Movies</a></section>"
                + pageEnd();
    }

    private static String errorPage(String message) {
        return pageStart("Notice")
                + "<section class='panel'><h1>Notice</h1><p>"
                + esc(message) + "</p><a class='btn' href='/movies'>Back to Movies</a>"
                + "</section>" + pageEnd();
    }

    private static List<String> cleanSeats(List<String> input) {
        Set<String> allowed = new HashSet<>(Arrays.asList(ALL_SEATS));
        LinkedHashSet<String> result = new LinkedHashSet<>();

        for (String seat : input) {
            if (allowed.contains(seat)) result.add(seat);
        }

        return new ArrayList<>(result);
    }

    private static String seatKey(String movie, String date,
                                  String show, String seat) {
        return movie + "|" + date + "|" + show + "|" + seat;
    }

    private static String releaseDisplay(MovieData m) {
        if (m.releaseDate == null || m.releaseDate.isEmpty()) {
            return "To be announced";
        }

        try {
            return LocalDate.parse(m.releaseDate, DATE_FORMAT)
                    .format(DISPLAY_DATE);
        } catch (DateTimeParseException e) {
            return m.releaseDate;
        }
    }

    private static String displayDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMAT).format(DISPLAY_DATE);
        } catch (Exception e) {
            return date;
        }
    }

    private static String hidden(String name, String value) {
        return "<input type='hidden' name='" + attr(name)
                + "' value='" + attr(value) + "'>";
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String attr(String value) {
        return esc(value);
    }

    private static String pageStart(String title) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>" + esc(title) + " | Movie Ticket Booking</title>"
                + "<style>"
                + "*{box-sizing:border-box}"
                + "body{margin:0;background:#10121b;color:#f4f4f8;"
                + "font-family:Arial,sans-serif;line-height:1.6}"
                + "nav{display:flex;flex-wrap:wrap;gap:20px;align-items:center;"
                + "padding:18px 6%;background:#191c29;border-bottom:1px solid #303448}"
                + "nav .brand{font-size:20px;font-weight:bold;color:#ffcf70;margin-right:auto}"
                + "a{color:#ffcf70;text-decoration:none}"
                + "nav a{color:#fff}"
                + "main{max-width:1150px;margin:auto;padding:28px 5% 60px}"
                + "h1{font-size:clamp(28px,5vw,44px);line-height:1.2}"
                + "h2{margin-top:30px}p{color:#d4d6e2}"
                + ".hero{padding:55px 25px;margin:10px 0 35px;border-radius:22px;"
                + "background:linear-gradient(130deg,#28213d,#172f42)}"
                + ".eyebrow,.tag{color:#ffcf70;font-weight:bold;letter-spacing:1px}"
                + ".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:18px}"
                + ".card,.panel{background:#1b1f2d;border:1px solid #34394e;"
                + "border-radius:16px;padding:22px;margin:16px 0}"
                + ".card h3{min-height:55px}"
                + ".muted{color:#9fa5ba;font-size:14px}"
                + ".btn,button{display:inline-block;border:0;border-radius:9px;"
                + "background:#ffcf70;color:#191621;font-weight:bold;padding:12px 17px;"
                + "margin:8px 6px 8px 0;cursor:pointer;font-size:15px}"
                + ".secondary{background:#393e53;color:white}"
                + ".warning{color:#ffb2a9}"
                + ".success{border-color:#4d9d77}"
                + "label{display:block;margin-top:14px;color:#e5e7ef}"
                + "select{width:100%;max-width:400px;padding:12px;margin:5px 0 15px;"
                + "border-radius:8px;background:#292e40;color:white;border:1px solid #4b5269}"
                + ".screen{background:#e4e6f1;color:#202331;text-align:center;"
                + "padding:8px;border-radius:50% 50% 8px 8px;margin:25px auto;"
                + "max-width:500px;font-weight:bold;letter-spacing:4px}"
                + ".seat-grid{display:grid;grid-template-columns:repeat(6,minmax(35px,1fr));"
                + "gap:10px;max-width:520px;margin:25px auto}"
                + ".seat{margin:0;position:relative;cursor:pointer}"
                + ".seat input{position:absolute;opacity:0}"
                + ".seat span{display:block;text-align:center;padding:10px 2px;"
                + "background:#294b43;border-radius:7px;font-size:13px}"
                + ".seat input:checked+span{background:#ffcf70;color:#191621}"
                + ".seat.occupied span{background:#5a3040;color:#ddd;cursor:not-allowed}"
                + "footer{text-align:center;padding:25px;color:#9fa5ba;"
                + "border-top:1px solid #303448}"
                + "@media(max-width:500px){nav{gap:12px}nav .brand{width:100%}.seat-grid{gap:6px}}"
                + "</style></head><body>"
                + "<nav><span class='brand'>CINEBOOK</span>"
                + "<a href='/home'>Home</a><a href='/movies'>Movies</a>"
                + "<a href='/history'>My Bookings</a><a href='/contact'>Contact</a>"
                + "</nav><main>";
    }

    private static String pageEnd() {
        return "</main><footer>Movie Ticket Booking System · Java AOOP Project"
                + "</footer></body></html>";
    }

    private static void sendHtml(HttpExchange exchange, String html)
            throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}