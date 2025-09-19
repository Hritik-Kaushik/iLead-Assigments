import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LibraNet Library Demo – Clean ASCII Output with Fine Summary
 */
public class LibraNetDemo {
    /* ---------------------------
       Enums & Interfaces
       --------------------------- */
    public enum AvailabilityStatus { AVAILABLE, BORROWED, ARCHIVED }

    public interface Playable {
        void play();
        void pause();
        void stop();
        long getPlaybackSeconds();
    }

    /* ---------------------------
       Exceptions
       --------------------------- */
    public static class LibraryException extends Exception { public LibraryException(String m){ super(m);} }
    public static class ItemNotFoundException extends LibraryException { public ItemNotFoundException(String m){ super(m);} }
    public static class ItemUnavailableException extends LibraryException { public ItemUnavailableException(String m){ super(m);} }
    public static class InvalidDurationFormatException extends LibraryException { public InvalidDurationFormatException(String m){ super(m);} }
    public static class InvalidIdFormatException extends LibraryException { public InvalidIdFormatException(String m){ super(m);} }
    public static class ItemNotBorrowedException extends LibraryException { public ItemNotBorrowedException(String m){ super(m);} }

    /* ---------------------------
       Models
       --------------------------- */
    public static abstract class LibraryItem {
        private final int id;
        private final String title, author;
        private AvailabilityStatus status;
        private final BigDecimal finePerDay = BigDecimal.valueOf(10); // fixed Rs.10/day
        private BorrowRecord currentBorrow;

        protected LibraryItem(int id, String title, String author) {
            this.id = id;
            this.title = Objects.requireNonNull(title);
            this.author = Objects.requireNonNull(author);
            this.status = AvailabilityStatus.AVAILABLE;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public AvailabilityStatus getStatus() { return status; }
        public boolean isAvailable() { return status == AvailabilityStatus.AVAILABLE; }
        protected void setStatus(AvailabilityStatus status) { this.status = status; }

        public BigDecimal getFinePerDay() { return finePerDay; }

        void attachBorrowRecord(BorrowRecord r) {
            this.currentBorrow = r;
            this.status = AvailabilityStatus.BORROWED;
        }
        BorrowRecord detachBorrowRecord() {
            BorrowRecord temp = currentBorrow;
            currentBorrow = null;
            this.status = AvailabilityStatus.AVAILABLE;
            return temp;
        }
        public BorrowRecord getCurrentBorrowRecord() { return currentBorrow; }

        @Override public String toString() {
            return String.format("%s: \"%s\" by %s (ID: %d) - Status: %s",
                    this.getClass().getSimpleName(), getTitle(), getAuthor(), getId(), getStatus());
        }
    }

    public static class Book extends LibraryItem {
        private final int pageCount;
        public Book(int id, String title, String author, int pageCount) {
            super(id, title, author);
            this.pageCount = pageCount;
        }
        public int getPageCount() { return pageCount; }
    }

    public static class Audiobook extends LibraryItem implements Playable {
        private final long playbackSeconds;
        private boolean isPlaying = false;
        public Audiobook(int id, String title, String author, long playbackSeconds) {
            super(id, title, author);
            this.playbackSeconds = playbackSeconds;
        }
        @Override public void play() { isPlaying = true; System.out.println("Playing audiobook: \"" + getTitle() + "\""); }
        @Override public void pause() { isPlaying = false; System.out.println("Paused: \"" + getTitle() + "\""); }
        @Override public void stop() { isPlaying = false; System.out.println("Stopped: \"" + getTitle() + "\""); }
        @Override public long getPlaybackSeconds() { return playbackSeconds; }
    }

    public static class EMagazine extends LibraryItem {
        private final int issueNumber;
        private boolean archived = false;
        public EMagazine(int id, String title, String author, int issueNumber) {
            super(id, title, author);
            this.issueNumber = issueNumber;
        }
        public int getIssueNumber() { return issueNumber; }
        public void archiveIssue() {
            if (!archived) {
                archived = true;
                setStatus(AvailabilityStatus.ARCHIVED);
                System.out.println("Archived e-magazine issue: " + issueNumber + " (\"" + getTitle() + "\")");
            }
        }
    }

    public static class User {
        private final int id; private final String name;
        public User(int id, String name) { this.id = id; this.name = name; }
        public int getId() { return id; } public String getName() { return name; }
        @Override public String toString() { return name + " (UserID=" + id + ")"; }
    }

    public static class BorrowRecord {
        private final int userId;
        private final int itemId;
        private final LocalDate borrowDate;
        private final LocalDate dueDate;
        private final BigDecimal finePerDayAtBorrow;

        public BorrowRecord(int userId, int itemId, LocalDate borrowDate, LocalDate dueDate, BigDecimal finePerDayAtBorrow) {
            this.userId = userId; this.itemId = itemId; this.borrowDate = borrowDate; this.dueDate = dueDate; this.finePerDayAtBorrow = finePerDayAtBorrow;
        }
        public int getUserId() { return userId; }
        public int getItemId() { return itemId; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public BigDecimal getFinePerDayAtBorrow() { return finePerDayAtBorrow; }

        @Override public String toString() {
            return String.format("Borrowed by User %d -> Item %d%n   Period: %s -> %s%n   Fine Rate: Rs.%s/day",
                    userId, itemId, borrowDate, dueDate, finePerDayAtBorrow.toPlainString());
        }
    }

    /* ---------------------------
       Utility Parsers
       --------------------------- */
    public static class IdParser {
        private static final Pattern DIGITS = Pattern.compile("^\\s*([0-9]+)\\s*$");
        public static int parseId(String raw) throws InvalidIdFormatException {
            if (raw == null) throw new InvalidIdFormatException("ID string is null");
            Matcher m = DIGITS.matcher(raw);
            if (!m.matches()) throw new InvalidIdFormatException("Invalid ID format: '" + raw + "'. Expect digits only.");
            try { return Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ex) { throw new InvalidIdFormatException("ID out of range: " + raw); }
        }
    }

    public static class DurationParser {
        private static final Pattern PATTERN = Pattern.compile("(?i)^\\s*(\\d+)\\s*(d|day|days|w|week|weeks)?\\s*$");

        public static long parseToDays(String raw) throws InvalidDurationFormatException {
            if (raw == null) throw new InvalidDurationFormatException("Duration string is null");
            Matcher m = PATTERN.matcher(raw.trim());
            if (!m.matches()) throw new InvalidDurationFormatException("Unrecognized duration format: '" + raw + "'");
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2);
            if (unit == null || unit.toLowerCase().startsWith("d")) {
                return value;
            } else if (unit.toLowerCase().startsWith("w")) {
                return value * 7L;
            } else {
                throw new InvalidDurationFormatException("Unsupported duration unit in: '" + raw + "'");
            }
        }
    }

    /* ---------------------------
       Catalog
       --------------------------- */
    public static class LibraryCatalog {
        private final Map<Integer, LibraryItem> items = new ConcurrentHashMap<>();
        private final Map<Integer, BigDecimal> finesByUser = new ConcurrentHashMap<>();

        public void addItem(LibraryItem item) {
            Objects.requireNonNull(item);
            items.put(item.getId(), item);
        }

        public LibraryItem getItem(int id) throws ItemNotFoundException {
            LibraryItem it = items.get(id);
            if (it == null) throw new ItemNotFoundException("No item with id: " + id);
            return it;
        }

        public BorrowRecord borrowItem(int itemId, User user, String durationStr) throws LibraryException {
            LibraryItem item = getItem(itemId);
            synchronized (item) {
                if (!item.isAvailable()) throw new ItemUnavailableException("Item not available: " + item);
                long days = DurationParser.parseToDays(durationStr);
                LocalDate borrowDate = LocalDate.now();
                LocalDate dueDate = borrowDate.plusDays(days);
                BorrowRecord record = new BorrowRecord(user.getId(), itemId, borrowDate, dueDate, item.getFinePerDay());
                item.attachBorrowRecord(record);
                return record;
            }
        }

        public BigDecimal returnItem(int itemId, LocalDate returnDate) throws LibraryException {
            LibraryItem item = getItem(itemId);
            synchronized (item) {
                BorrowRecord record = item.getCurrentBorrowRecord();
                if (record == null) throw new ItemNotBorrowedException("Item not currently borrowed: " + item);
                LocalDate due = record.getDueDate();
                long overdueDays = ChronoUnit.DAYS.between(due, returnDate);
                BigDecimal fine = BigDecimal.ZERO;
                if (overdueDays > 0) {
                    fine = record.getFinePerDayAtBorrow().multiply(BigDecimal.valueOf(overdueDays));
                    finesByUser.merge(record.getUserId(), fine, BigDecimal::add);
                    System.out.printf("User %d returned Item %d late by %d days -> Fine: Rs.%s%n",
                            record.getUserId(), record.getItemId(), overdueDays, fine.toPlainString());
                } else {
                    System.out.printf("User %d returned Item %d on time. No fine.%n", record.getUserId(), record.getItemId());
                }
                item.detachBorrowRecord();
                return fine;
            }
        }

        public List<LibraryItem> searchByType(Class<? extends LibraryItem> type) {
            List<LibraryItem> out = new ArrayList<>();
            for (LibraryItem it : items.values()) if (type.isInstance(it)) out.add(it);
            return out;
        }

        public BigDecimal getAccumulatedFineForUser(int userId) {
            return finesByUser.getOrDefault(userId, BigDecimal.ZERO);
        }
    }

    /* ---------------------------
       Demo / main
       --------------------------- */
    public static void main(String[] args) {
        try {
            LibraryCatalog catalog = new LibraryCatalog();

            // Create items
            Book b1 = new Book(IdParser.parseId("101"), "Clean Code", "Robert C. Martin", 464);
            Audiobook a1 = new Audiobook(IdParser.parseId("202"), "Effective Java (Audio)", "Joshua Bloch", 3600);
            EMagazine m1 = new EMagazine(IdParser.parseId("303"), "Tech Monthly", "Editor Team", 42);

            catalog.addItem(b1); catalog.addItem(a1); catalog.addItem(m1);

            User u1 = new User(1, "Aisha");
            User u2 = new User(2, "Vikram");

            // Borrow book
            System.out.println(catalog.borrowItem(101, u1, "14 days"));

            // Attempting to borrow same book should fail
            try {
                catalog.borrowItem(101, u2, "7 days");
            } catch (ItemUnavailableException ex) {
                System.out.println("Failure: " + ex.getMessage());
            }

            // Borrow audiobook
            System.out.println(catalog.borrowItem(202, u2, "7 days"));
            a1.play();
            a1.pause();

            // Return book late by 3 days
            LocalDate lateReturnDate = LocalDate.now().plusDays(17);
            catalog.returnItem(101, lateReturnDate);

            // Archive e-magazine
            m1.archiveIssue();

            // Search example
            System.out.println("\nAudiobooks in catalog:");
            for (LibraryItem it : catalog.searchByType(Audiobook.class)) {
                System.out.println("   - " + it);
            }

            // Bad ID parsing
            try { IdParser.parseId("12a"); } catch (InvalidIdFormatException ex) { System.out.println("Error: " + ex.getMessage()); }

            // Bad duration parsing
            try { DurationParser.parseToDays("3fortnights"); } catch (InvalidDurationFormatException ex) { System.out.println("Error: " + ex.getMessage()); }

            // Fine summary
            System.out.println("\nUser fines summary:");
            System.out.printf("- %s: Rs.%s%n", u1, catalog.getAccumulatedFineForUser(u1.getId()).toPlainString());
            System.out.printf("- %s: Rs.%s%n", u2, catalog.getAccumulatedFineForUser(u2.getId()).toPlainString());

        } catch (LibraryException ex) {
            System.err.println("Library error: " + ex.getMessage());
        }
    }
}
