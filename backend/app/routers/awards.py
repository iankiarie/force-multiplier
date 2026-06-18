from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from app import crud, schemas
from app.deps import get_db, get_current_active_user, get_current_active_superuser

router = APIRouter()


@router.post("/", response_model=schemas.Award, status_code=status.HTTP_201_CREATED)
def create_award(
    award_in: schemas.AwardCreate,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_superuser)  # Only admin can create awards
):
    # In a real app, we might want to check if the user exists, but the CRUD function will return None if not.
    db_award = crud.create_award(db=db, award_in=award_in, user_id=current_user.id)
    if not db_award:
        raise HTTPException(status_code=400, detail="User not found")
    return db_award


@router.get("/", response_model=List[schemas.Award])
def read_awards(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    # For now, we return all awards. Later we might want to filter by month or user.
    awards = crud.get_awards(db, skip=skip, limit=limit)
    return awards


@router.get("/{award_id}", response_model=schemas.Award)
def read_award(
    award_id: int,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    award = crud.get_award(db, award_id=award_id)
    if not award:
        raise HTTPException(status_code=404, detail="Award not found")
    # Optional: check if the award belongs to the current user (if not admin)
    if award.user_id != current_user.id and current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not enough permissions")
    return award


@router.get("/month/{month}", response_model=List[schemas.Award])
def read_awards_by_month(
    month: str,  # Format: YYYY-MM
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    awards = crud.get_awards_by_month(db, month=month)
    return awards


# We don't have update/delete for awards in the spec, but we can leave them out for now.