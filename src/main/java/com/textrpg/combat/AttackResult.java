package com.textrpg.combat;

public record AttackResult(boolean hit, boolean critical, int damage, boolean killed) {}
