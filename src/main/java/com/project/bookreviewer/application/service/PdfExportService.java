package com.project.bookreviewer.application.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.UserBookStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfExportService {
    private final UserBookStatusService statusService;
    private final BookService bookService;
    private final UserService userService;

    public byte[] exportReadingList(Long userId) {
        var user = userService.getUserById(userId);
        List<UserBookStatus> allStatuses = statusService.getUserLibrary(userId, null);
        Map<ReadingStatus, List<UserBookStatus>> byStatus = allStatuses.stream()
                .collect(Collectors.groupingBy(UserBookStatus::getStatus));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph(user.getUsername() + "'s Reading List", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Shelves
            addShelf(document, "Want to Read", byStatus.get(ReadingStatus.WANT_TO_READ));
            addShelf(document, "Currently Reading", byStatus.get(ReadingStatus.READING));
            addShelf(document, "Read", byStatus.get(ReadingStatus.READ));
            addShelf(document, "Abandoned", byStatus.get(ReadingStatus.ABANDONED));

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
        return baos.toByteArray();
    }

    private void addShelf(Document document, String shelfName, List<UserBookStatus> statuses) throws DocumentException {
        if (statuses == null || statuses.isEmpty()) return;

        Font shelfFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph shelfTitle = new Paragraph(shelfName, shelfFont);
        shelfTitle.setSpacingBefore(10);
        document.add(shelfTitle);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setWidths(new float[]{2f, 2f, 1f});

        // Headers
        table.addCell(new Phrase("Title", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        table.addCell(new Phrase("Author", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        table.addCell(new Phrase("Year", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));

        for (UserBookStatus status : statuses) {
            Book book = bookService.getBook(status.getBookId());
            table.addCell(book.getTitle());
            table.addCell(book.getAuthor());
            table.addCell(book.getPublicationYear() != null ? book.getPublicationYear().toString() : "-");
        }
        document.add(table);
    }
}