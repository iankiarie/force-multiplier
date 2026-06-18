from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from app import crud, schemas
from app.deps import get_db, get_current_active_user

router = APIRouter()


@router.post("/", response_model=schemas.Note, status_code=status.HTTP_201_CREATED)
def create_note(
    note_in: schemas.NoteCreate,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    # Only allow creating notes for the current user
    if note_in.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not enough permissions")
    return crud.create_note(db=db, note_in=note_in, user_id=current_user.id)


@router.get("/", response_model=List[schemas.Note])
def read_notes(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    notes = crud.get_notes_by_user(db, user_id=current_user.id, skip=skip, limit=limit)
    return notes


@router.get("/search", response_model=List[schemas.Note])
def search_notes(
    q: str,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    notes = crud.search_notes_by_user(db, user_id=current_user.id, query=q)
    return notes


@router.get("/{note_id}", response_model=schemas.Note)
def read_note(
    note_id: int,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    note = crud.get_note(db, note_id=note_id)
    if not note:
        raise HTTPException(status_code=404, detail="Note not found")
    # Check ownership
    if note.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not enough permissions")
    return note


@router.patch("/{note_id}", response_model=schemas.Note)
def update_note(
    note_id: int,
    note_in: schemas.NoteUpdate,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    db_note = crud.get_note(db, note_id=note_id)
    if not db_note:
        raise HTTPException(status_code=404, detail="Note not found")
    # Check ownership
    if db_note.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not enough permissions")
    return crud.update_note(db=db, db_note=db_note, note_in=note_in)


@router.delete("/{note_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_note(
    note_id: int,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    db_note = crud.get_note(db, note_id=note_id)
    if not db_note:
        raise HTTPException(status_code=404, detail="Note not found")
    # Check ownership
    if db_note.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not enough permissions")
    crud.delete_note(db=db, note_id=note_id)
    return None