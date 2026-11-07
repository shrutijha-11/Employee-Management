package com.shruti.EmpManage.Exception;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException(Long id, Long sentVersion, Long currentVersion) {
        super("Employee " + id + " was changed by someone else: you sent version "
                + sentVersion + " but the current version is " + currentVersion);
    }
}
