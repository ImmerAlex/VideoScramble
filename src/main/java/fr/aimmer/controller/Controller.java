package fr.aimmer.controller;

import javafx.scene.Scene;

import java.util.function.Supplier;


@FunctionalInterface
public interface Controller extends Supplier<Scene>
{
	@Override
	Scene get();
}
