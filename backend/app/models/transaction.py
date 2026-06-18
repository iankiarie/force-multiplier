from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
import enum

from app.database import Base
from .user import User


class TransactionType(str, enum.Enum):
    BET_PLACED = "bet_placed"
    BET_WIN = "bet_win"
    BONUS = "bonus"
    ADJUSTMENT = "adjustment"


class Transaction(Base):
    __tablename__ = "transactions"

    id = Column(Integer, primary_key=True, index=True)
    amount = Column(Integer, nullable=False)  # Can be negative or positive
    type = Column(Enum(TransactionType), nullable=False)
    description = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)

    # Relationships
    user = relationship("User", back_populates="transactions")