package com.jmbross.payroll.service;

import com.jmbross.payroll.domain.Session;

public final class AuthorizationService {
    public void requireAdmin(Session session) {
        if (session == null || !session.isAdmin()) {
            throw new SecurityException("Administrator role required");
        }
    }
}
