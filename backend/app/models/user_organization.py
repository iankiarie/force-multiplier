"""
UserOrganization association model.
"""
from sqlalchemy import Boolean, Column, ForeignKey, Integer
from sqlalchemy.orm import relationship

from app.database import Base


class UserOrganization(Base):
    __tablename__ = "user_organizations"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False)
    role_id = Column(Integer, ForeignKey("roles.id"), nullable=False)
    is_active = Column(Boolean, default=True)
    # We can add a flag for primary organization if needed, but not required for now.

    # Relationships
    user = relationship("User", back_populates="organizations")
    organization = relationship("Organization", back_populates="users")
    role = relationship("Role", back_populates="user_organizations")

    def __repr__(self):
        return f"<UserOrganization(user_id={self.user_id}, organization_id={self.organization_id}, role_id={self.role_id})>"