from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, Enum
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
import enum

from app.database import Base


class PredictionOutcome(str, enum.Enum):
    TEAM_A = "team_a"
    TEAM_B = "team_b"
    DRAW = "draw"


class Prediction(Base):
    __tablename__ = "predictions"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    outcome = Column(Enum(PredictionOutcome), nullable=True)  # Actual outcome, set after resolution
    deadline = Column(DateTime(timezone=True), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    resolved_at = Column(DateTime(timezone=True), nullable=True)
    created_by = Column(Integer, ForeignKey("users.id"), nullable=False)

    # Relationships
    creator = relationship("User", back_populates="predictions")
    bets = relationship("Bet", back_populates="prediction")