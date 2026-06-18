from sqlalchemy.orm import Session
from app.models import bet, prediction
from app.schemas import bet as bet_schema
from app.crud.user import get_user
from app.crud.prediction import get_prediction
from datetime import datetime


def get_bet(db: Session, bet_id: int):
    return db.query(bet.Bet).filter(bet.Bet.id == bet_id).first()


def get_bets_by_user(db: Session, user_id: int, skip: int = 0, limit: int = 100):
    return db.query(bet.Bet).filter(bet.Bet.user_id == user_id).offset(skip).limit(limit).all()


def get_bets_by_prediction(db: Session, prediction_id: int, skip: int = 0, limit: int = 100):
    return db.query(bet.Bet).filter(bet.Bet.prediction_id == prediction_id).offset(skip).limit(limit).all()


def create_bet(db: Session, bet_in: bet_schema.BetCreate, user_id: int):
    # Verify user and prediction exist
    user = get_user(db, user_id)
    if not user:
        return None
    prediction = get_prediction(db, bet_in.prediction_id)
    if not prediction:
        return None
    # Optional: check if prediction is already resolved
    if prediction.outcome is not None:
        return None  # Cannot bet on resolved prediction

    db_bet = bet.Bet(
        stake=bet_in.stake,
        chosen_outcome=bet_in.chosen_outcome.value,  # Store as string
        user_id=user_id,
        prediction_id=bet_in.prediction_id,
    )
    db.add(db_bet)
    db.commit()
    db.refresh(db_bet)
    return db_bet


def settle_bet(db: Session, bet_id: int, actual_outcome: str):
    db_bet = get_bet(db, bet_id)
    if not db_bet or db_bet.settled:
        return None
    prediction = get_prediction(db, db_bet.prediction_id)
    if not prediction:
        return None

    # Mark bet as settled
    db_bet.settled = True
    db_bet.settled_at = datetime.utcnow()

    # Determine if user won
    is_win = db_bet.chosen_outcome == actual_outcome

    # If win, credit double stake; if lose, stake already deducted at bet placement (handled elsewhere)
    if is_win:
        # Credit stake * 2 as BET_WIN
        from app.crud.transaction import create_transaction
        create_transaction(
            db,
            amount=db_bet.stake * 2,
            type_="bet_win",
            description=f"Win for bet on prediction {prediction.title}",
            user_id=db_bet.user_id,
        )
        # Also add points to user? In our model, points are balance from transactions.
        # The transaction will update the balance via the amount.
    # If lose, no further transaction (the debit already happened at bet placement)

    db.add(db_bet)
    db.commit()
    db.refresh(db_bet)
    return db_bet