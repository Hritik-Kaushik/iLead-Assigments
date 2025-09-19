This program is a mini library management system (LibraNet) written in Java. It demonstrates object-oriented design, error handling, and extensibility.

Entities (Classes):

LibraryItem (abstract) → base class for all items (Book, Audiobook, EMagazine).

Common attributes: id, title, author, status, finePerDay.

Common operations: borrow, return, track status.

Book → adds pageCount.

Audiobook → implements Playable interface with play, pause, stop, and playbackSeconds.

EMagazine → adds issueNumber and supports archiveIssue().

User → represents library users (with id, name).

BorrowRecord → tracks borrowing details: userId, itemId, borrowDate, dueDate, and fine rate at the time of borrowing.

Interfaces/Utilities:

Playable → defines how an audiobook can be played/paused/stopped.

IdParser → validates and parses item IDs (must be all digits).

DurationParser → parses durations like "14 days" or "2 weeks" into a number of days.

Error Handling (Custom Exceptions):

ItemNotFoundException, ItemUnavailableException, InvalidDurationFormatException, etc.
These ensure the system reacts gracefully to wrong input.

LibraryCatalog:

Manages items and fines.

borrowItem(...) → creates a borrow record if item is available.

returnItem(...) → calculates fines if returned late.

searchByType(...) → lets you search (e.g., all Audiobooks).

Tracks fines per user.

Main Demo (in main):

Adds a book, audiobook, and magazine.

Creates two users: Aisha and Vikram.

Simulates borrowing/returning items, playing audiobook, archiving magazine, and shows error handling.

Finally prints a fine summary.
