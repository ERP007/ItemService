package com.fallguys.itemservice.domain;

public interface UserActivityEventPublisher {

    void publish(Item item, UserActivityAction action, String employeeNo, String status);
}
