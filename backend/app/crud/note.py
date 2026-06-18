from sqlalchemy.orm import Session
from app.models import note
from app.schemas import note as note_schema


def get_note(db: Session, note_id: int):
    return db.query(note.Note).filter(note.Note.id == note_id).first()


def get_notes_by_user(db: Session, user_id: int, skip: int = 0, limit: int = 100):
    return db.query(note.Note).filter(note.Note.user_id == user_id).offset(skip).limit(limit).all()


def search_notes_by_user(db: Session, user_id: int, query: str, skip: int = 0, limit: int = 100):
    # Simple search: title or content contains the query (case-insensitive)
    search_term = f"%{query}%"
    return db.query(note.Note).filter(
        note.Note.user_id == user_id,
        (note.Note.title.ilike(search_term)) | (note.Note.content.ilike(search_term))
    ).offset(skip).limit(limit).all()


def create_note(db: Session, note_in: note_schema.NoteCreate, user_id: int):
    db_note = note.Note(
        title=note_in.title,
        content=note_in.content,
        tags=(",".join(note_in.tags) if note_in.tags else None),
        user_id=user_id,
    )
    db.add(db_note)
    db.commit()
    db.refresh(db_note)
    return db_note


def update_note(db: Session, db_note: note.Note, note_in: note_schema.NoteUpdate):
    update_data = note_in.dict(exclude_unset=True)
    if "tags" in update_data and update_data["tags"] is not None:
        update_data["tags"] = ",".join(update_data["tags"])
    for field, value in update_data.items():
        setattr(db_note, field, value)
    db.add(db_note)
    db.commit()
    db.refresh(db_note)
    return db_note


def delete_note(db: Session, note_id: int):
    db_note = db.query(note.Note).filter(note.Note.id == note_id).first()
    if db_note:
        db.delete(db_note)
        db.commit()
    return db_note