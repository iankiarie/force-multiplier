"""
Role model.
"""
from sqlalchemy import Boolean, Column, Integer, String
from sqlalchemy.orm import relationship

from app.database import Base


class Role(Base):
    __tablename__ = "roles"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True, nullable=False)  # e.g., admin, manager, employee
    description = Column(String, nullable=True)
    is_active = Column(Boolean, default=True)

    # Relationships
    user_organizations = relationship("UserOrganization", back_populates="role")

    def __repr__(self):
        return f"<Role(id={self.id}, name='{self.name}')>"