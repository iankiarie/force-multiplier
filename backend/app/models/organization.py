"""
Organization model.
"""
from sqlalchemy import Boolean, Column, Integer, String
from sqlalchemy.orm import relationship

from app.database import Base


class Organization(Base):
    __tablename__ = "organizations"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True, nullable=False)
    description = Column(String, nullable=True)
    is_active = Column(Boolean, default=True)
    # We can add more fields like created_at, updated_at if needed

    # Relationships
    users = relationship("UserOrganization", back_populates="organization")
    # We'll also have a direct many-to-many to User via the association table
    # but we'll define it in the User model for simplicity.

    def __repr__(self):
        return f"<Organization(id={self.id}, name='{self.name}')>"