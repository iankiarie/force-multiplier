from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Float, Enum, Boolean
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
import enum

from app.database import Base
from .user import User
from .prediction import Prediction


class TransactionType(str, enum.Enum):
    BET_PLACED = "bet_placed"
    BET_WIN = "bet_win"
    BONUS = "bonus"
    ADJUSTMENT = "adjustment"


class Bet(Base):
    __tablename__ = "bets"

    id = Column(Integer, primary_key=True, index=True)
    stake = Column(Integer, nullable=False)  # Amount staked
    chosen_outcome = Column(String, nullable=False)  # team_a, team_b, draw
    settled = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    settled_at = Column(DateTime(timezone=True), nullable=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    prediction_id = Column(Integer, ForeignKey("predictions.id"), nullable=False)

    # Relationships
    user = relationship("User", back_populates="bets")
    prediction = relationship("Prediction", back_populates="bets")