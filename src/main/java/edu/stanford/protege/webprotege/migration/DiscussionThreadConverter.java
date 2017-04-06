package edu.stanford.protege.webprotege.migration;

import edu.stanford.bmir.protege.web.shared.notes.DiscussionThread;
import edu.stanford.bmir.protege.web.shared.notes.Note;
import edu.stanford.bmir.protege.web.shared.notes.NoteStatus;
import org.bson.Document;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 6 Apr 2017
 */
public class DiscussionThreadConverter {

    private static final String ID = "_id";

    private static final String PROJECT_ID = "projectId";

    private static final String TYPE = "type";

    private static final String IRI = "iri";

    private static final String ENTITY = "entity";

    private static final String STATUS = "status";

    private static final String COMMENTS = "comments";

    private static final String OPEN = "OPEN";

    private static final String CLOSED = "CLOSED";

    @Nonnull
    private final String projectId;

    @Nonnull
    private final OWLEntity targetEntity;

    @Nonnull
    private final DiscussionThread discussionThread;

    public DiscussionThreadConverter(@Nonnull String projectId,
                                     @Nonnull OWLEntity targetEntity,
                                     @Nonnull DiscussionThread discussionThread) {
        this.projectId = checkNotNull(projectId);
        this.targetEntity = checkNotNull(targetEntity);
        this.discussionThread = checkNotNull(discussionThread);
    }

    public List<Document> convert() {
        return discussionThread.getRootNotes().stream()
                               .map(this::convertToThread)
                               .collect(toList());
    }

    private Document convertToThread(Note rootNote) {
        /*
               {
                    "_id" : "UUID",
                    "projectId" : "ProjectId",
                    "entity" : {
                        "type" : "Type name",
                        "iri" : "IRI"
                    },
                    "status" : "CLOSED",
                    "comments" : [
                        {
                            "id" : "UUID",
                            "createdBy" : "UserId",
                            "createdAt" : timestamp,
                            "body" : "String"
                        }
                    ]
                }
         */

        Document threadDocument = new Document(ID, UUID.randomUUID().toString());
        threadDocument.append(PROJECT_ID, projectId);

        Document entityDocument = new Document();
        entityDocument.append(TYPE, targetEntity.getEntityType().getName());
        entityDocument.append(IRI, targetEntity.getIRI().toString());
        threadDocument.append(ENTITY, entityDocument);

        threadDocument.append(STATUS, convertNoteStatus(rootNote));

        List<Document> commentDocuments = new ArrayList<>();
        convertNotesToComments(discussionThread, rootNote, commentDocuments);
        threadDocument.append(COMMENTS, commentDocuments);
        return threadDocument;
    }

    private static void convertNotesToComments(DiscussionThread thread, Note note, List<Document> documents) {
        /*
                        {
                            "id" : "UUID",
                            "createdBy" : "UserId",
                            "createdAt" : timestamp,
                            "body" : "String"
                        }
         */
        Document commentDocument = new Document("_id" , UUID.randomUUID().toString());
        commentDocument.append("createdBy" , note.getAuthor().getUserName());
        commentDocument.append("createdAt" , note.getTimestamp());
        String subject = note.getSubject();
        String body = "";
        if (!subject.isEmpty()) {
            body += subject;
            body += "\n\n";
        }
        body += note.getBody();
        commentDocument.append("body" , body);
        documents.add(commentDocument);
        for (Note reply : thread.getReplies(note.getNoteId())) {
            convertNotesToComments(thread, reply, documents);
        }
    }


    private static String convertNoteStatus(Note note) {
        NoteStatus noteStatus = note.getContent().getNoteStatus().or(NoteStatus.OPEN);
        return noteStatus == NoteStatus.OPEN ? OPEN : CLOSED;
    }
}
