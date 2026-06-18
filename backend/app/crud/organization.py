"""
CRUD operations for Organization model.
"""
from sqlalchemy.orm import Session

from app.models import organization
from app.schemas import organization as organization_schema


def get_organization(db: Session, organization_id: int):
    return db.query(organization.Organization).filter(organization.Organization.id == organization_id).first()


def get_organization_by_name(db: Session, name: str):
    return db.query(organization.Organization).filter(organization.Organization.name == name).first()


def get_organizations(db: Session, skip: int = 0, limit: int = 100):
    return db.query(organization.Organization).offset(skip).limit(limit).all()


def create_organization(db: Session, organization_in: organization_schema.OrganizationCreate):
    db_organization = organization.Organization(
        name=organization_in.name,
        description=organization_in.description,
        is_active=organization_in.is_active,
    )
    db.add(db_organization)
    db.commit()
    db.refresh(db_organization)
    return db_organization


def update_organization(db: Session, db_organization: organization.Organization, organization_in: organization_schema.OrganizationUpdate):
    update_data = organization_in.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_organization, field, value)
    db.add(db_organization)
    db.commit()
    db.refresh(db_organization)
    return db_organization


def delete_organization(db: Session, organization_id: int):
    db_organization = db.query(organization.Organization).filter(organization.Organization.id == organization_id).first()
    if db_organization:
        db.delete(db_organization)
        db.commit()
    return db_organization