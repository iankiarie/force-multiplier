"""
User model.
"""
import enum
from sqlalchemy import Boolean, Column, DateTime, ForeignKey, Integer, String, Enum
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class UserRole(str, enum.Enum):
    ADMIN = "admin"
    USER = "user"


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    username = Column(String, unique=True, index=True, nullable=False)
    full_name = Column(String, nullable=True)
    hashed_password = Column(String, nullable=False)
    is_active = Column(Boolean, default=True)
    role = Column(Enum(UserRole), default=UserRole.USER)
    points = Column(Integer, default=0)

    # Relationships
    organizations = relationship("UserOrganization", back_populates="user")
    predictions = relationship("Prediction", back_populates="creator")
    bets = relationship("Bet", back_populates="user")
    transactions = relationship("Transaction", back_populates="user")
    notes = relationship("Note", back_populates="user")
    awards = relationship("Award", back_populates="user")

    def __repr__(self):
        return f"<User(id={self.id}, email='{self.email}', username='{self.username}')>"