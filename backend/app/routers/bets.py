from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from app import crud, schemas
from app.deps import get_db, get_current_active_user, get_current_active_superuser

router = APIRouter()


@router.post("/", response_model=schemas.Bet, status_code=status.HTTP_201_CREATED)
def create_bet(
    bet_in: schemas.BetCreate,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    db_bet = crud.create_bet(db=db, bet_in=bet_in, user_id=current_user.id)
    if not db_bet:
        raise HTTPException(status_code=400, detail="Cannot create bet. User or prediction not found, or prediction already resolved.")
    return db_bet


@router.get("/", response_model=List[schemas.Bet])
def read_bets(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    bets = crud.get_bets_by_user(db, user_id=current_user.id, skip=skip, limit=limit)
    return bets


@router.get("/{bet_id}", response_model=schemas.Bet)
def read_bet(
    bet_id: int,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    bet = crud.get_bet(db, bet_id=bet_id)
    if not bet:
        raise HTTPException(status_code=404, detail="Bet not found")
    # Optional: check if the bet belongs to the current user
    if bet.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not enough permissions")
    return bet


# Note: We don't have an update bet endpoint because bets are usually not updated after placement.
# We have a settle bet endpoint for admin or system to settle a bet.
@router.post("/{bet_id}/settle", response_model=schemas.Bet)
def settle_bet(
    bet_id: int,
    outcome: str,  # team_a, team_b, draw
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_superuser)  # Only admin can settle bets
):
    db_bet = crud.settle_bet(db=db, bet_id=bet_id, actual_outcome=outcome)
    if not db_bet:
        raise HTTPException(status_code=400, detail="Cannot settle bet. Bet not found or already settled.")
    return db_bet