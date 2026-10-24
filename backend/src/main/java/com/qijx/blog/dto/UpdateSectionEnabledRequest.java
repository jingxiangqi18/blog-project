package com.qijx.blog.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSectionEnabledRequest(@NotNull Boolean enabled) {}
