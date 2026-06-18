"""
CRUD operations for UserOrganization model.
"""
from sqlalchemy.orm import Session

from app.models import user_organization
from app.schemas import user_organization as user_organization_schema


def get_user_organization(db: Session, user_organization_id: int):
    return db.query(user_organization.UserOrganization).filter(user_organization.UserOrganization.id == user_organization_id).first()


def get_user_organizations_by_user(db: Session, user_id: int):
    return db.query(user_organization.UserOrganization).filter(user_organization.UserOrganization.user_id == user_id).all()


def get_user_organizations_by_organization(db: Session, organization_id: int):
    return db.query(user_organization.UserOrganization).filter(user_organization.UserOrganization.organization_id == organization_id).all()


def create_user_organization(db: Session, user_organization_in: user_organization_schema.UserOrganizationCreate):
    db_user_organization = user_organization.UserOrganization(
        user_id=user_organization_in.user_id,
        organization_id=user_organization_in.organization_id,
        role_id=user_organization_in.role_id,
        is_active=user_organization_in.is_active,
    )
    db.add(db_user_organization)
    db.commit()
    db.refresh(db_user_organization)
    return db_user_organization


def update_user_organization(db: Session, db_user_organization: user_organization.UserOrganization, user_organization_in: user_organization_schema.UserOrganizationUpdate):
    update_data = user_organization_in.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_user_organization, field, value)
    db.add(db_user_organization)
    db.commit()
    db.refresh(db_user_organization)
    return db_user_organization


def delete_user_organization(db: Session, user_organization_id: int):
    db_user_organization = db.query(user_organization.UserOrganization).filter(user_organization.UserOrganization.id == user_organization_id).first()
    if db_user_organization:
        db.delete(db_user_organization)
        db.commit()
    return db_user_organization