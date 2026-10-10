package com.robvanblerk.tieredpower.client.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.client.screen.MachineScreen;
import com.robvanblerk.tieredpower.recipe.AlloyingRecipe;
import com.robvanblerk.tieredpower.recipe.MachineRecipe;
import com.robvanblerk.tieredpower.recipe.PulverizingRecipe;
import com.robvanblerk.tieredpower.registry.ModRecipes;

/**
 * The in-game Tiered Industries Guide. Left: search and categories. Right: the category's items, or one item's page with its
 * description, key numbers and its real recipes (read live from the game, so modpack changes show up).
 */
public class GuideScreen extends Screen {
	private static final int W = 330, H = 210, SIDE = 112;
	private static final int PANEL = 0xFFC6C6C6, DARK = 0xFF555555, INK = 0xFF2B2B2B, SOFT = 0xFF555555, ACCENT = 0xFF8A2A1F;

	private int left, top;
	private EditBox search;
	private GuideData.Category category;   // null = "Getting started"
	private GuideData.Entry entry;          // null = show the list
	private double scroll;
	private int contentHeight;

	// Clickable things collected while drawing, checked on click.
	private record Hit(int x, int y, int w, int h, Runnable action) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private final List<Hit> hits = new ArrayList<>();
	private ItemStack hovered = ItemStack.EMPTY;

	public GuideScreen() {
		super(Component.literal("Tiered Industries Guide"));
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		search = new EditBox(font, left + 8, top + 20, SIDE - 12, 14, Component.literal("Search"));
		search.setHint(Component.literal("Search..."));
		search.setResponder(s -> {
			scroll = 0;
			entry = null;
		});
		addRenderableWidget(search);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void open(GuideData.Entry e) {
		entry = e;
		scroll = 0;
		click();
	}

	private void click() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
	}

	// ------------------------------------------------------------ drawing
	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		hits.clear();
		hovered = ItemStack.EMPTY;
		GuideData.Book book = GuideData.get();

		// Book body: a GUI-style panel with a darker spine between the columns.
		g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF000000);
		g.fill(left, top, left + W, top + H, PANEL);
		g.fill(left, top, left + W, top + 1, 0xFFFFFFFF);
		g.fill(left, top, left + 1, top + H, 0xFFFFFFFF);
		g.fill(left + SIDE, top + 4, left + SIDE + 2, top + H - 4, DARK);
		g.drawString(font, "Tiered Industries", left + 8, top + 7, ACCENT, false);
		g.drawString(font, "v" + book.version(), left + SIDE - 8 - font.width("v" + book.version()), top + 7, SOFT, false);

		drawSidebar(g, book, mouseX, mouseY);

		int px = left + SIDE + 10, py = top + 8, pw = W - SIDE - 18, ph = H - 16;
		g.enableScissor(px, py, px + pw, py + ph);
		int y = py - (int) scroll;
		int start = y;
		String query = search.getValue().trim().toLowerCase(Locale.ROOT);
		if (!query.isEmpty()) y = drawSearchResults(g, book, query, px, y, pw, mouseX, mouseY);
		else if (entry != null) y = drawEntry(g, entry, px, y, pw, mouseX, mouseY);
		else if (category != null) y = drawCategory(g, category, px, y, pw, mouseX, mouseY);
		else y = drawStart(g, book, px, y, pw);
		contentHeight = y - start;
		g.disableScissor();

		// Scroll bar
		if (contentHeight > ph) {
			int barH = Math.max(12, ph * ph / contentHeight);
			int barY = py + (int) ((ph - barH) * (scroll / (contentHeight - ph)));
			g.fill(left + W - 6, py, left + W - 3, py + ph, 0xFFA0A0A0);
			g.fill(left + W - 6, barY, left + W - 3, barY + barH, DARK);
		}

		super.render(g, mouseX, mouseY, partialTick);
		if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mouseX, mouseY);
	}

	private void drawSidebar(GuiGraphics g, GuideData.Book book, int mx, int my) {
		int y = top + 42;
		y = sideLink(g, "Getting started", category == null && entry == null, y, mx, my, () -> {
			category = null;
			entry = null;
			scroll = 0;
			search.setValue("");
			click();
		});
		for (GuideData.Category c : book.categories()) {
			final GuideData.Category cat = c;
			y = sideLink(g, c.title(), category == c && search.getValue().isEmpty(), y, mx, my, () -> {
				category = cat;
				entry = null;
				scroll = 0;
				search.setValue("");
				click();
			});
		}
	}

	private int sideLink(GuiGraphics g, String text, boolean active, int y, int mx, int my, Runnable action) {
		int x = left + 6, w = SIDE - 10, h = 13;
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
		if (active) g.fill(x, y, x + w, y + h, 0xFFB0B0B0);
		if (active || hover) g.fill(x, y, x + 2, y + h, ACCENT);
		String label = font.plainSubstrByWidth(text, w - 8);
		g.drawString(font, label, x + 5, y + 3, active ? INK : (hover ? INK : SOFT), false);
		hits.add(new Hit(x, y, w, h, action));
		return y + h + 1;
	}

	private int heading(GuiGraphics g, String text, int x, int y) {
		g.drawString(font, text, x, y, ACCENT, false);
		return y + 12;
	}

	private int paragraph(GuiGraphics g, String text, int x, int y, int w, int colour) {
		for (FormattedCharSequence line : font.split(Component.literal(text), w)) {
			g.drawString(font, line, x, y, colour, false);
			y += 10;
		}
		return y + 3;
	}

	private int drawStart(GuiGraphics g, GuideData.Book book, int x, int y, int w) {
		y = heading(g, "Getting started", x, y);
		y = paragraph(g, "Each step pays for the next. Pick a category on the left, or search.", x, y, w, SOFT);
		int n = 1;
		for (GuideData.Step s : book.start()) {
			g.drawString(font, n++ + ". " + s.title(), x, y, INK, false);
			y = paragraph(g, s.text(), x + 10, y + 11, w - 10, SOFT);
		}
		return y;
	}

	private int drawCategory(GuiGraphics g, GuideData.Category c, int x, int y, int w, int mx, int my) {
		y = heading(g, c.title(), x, y);
		if (!c.intro().isEmpty()) y = paragraph(g, c.intro(), x, y, w, SOFT);
		return drawEntryList(g, c.entries(), x, y, w, mx, my);
	}

	private int drawSearchResults(GuiGraphics g, GuideData.Book book, String query, int x, int y, int w, int mx, int my) {
		if (entry != null) return drawEntry(g, entry, x, y, w, mx, my);
		List<GuideData.Entry> found = new ArrayList<>();
		for (GuideData.Category c : book.categories()) {
			for (GuideData.Entry e : c.entries()) {
				if (e.name().toLowerCase(Locale.ROOT).contains(query) || e.text().toLowerCase(Locale.ROOT).contains(query)) found.add(e);
			}
		}
		y = heading(g, found.isEmpty() ? "Nothing matches" : found.size() + " found", x, y);
		return drawEntryList(g, found, x, y, w, mx, my);
	}

	private int drawEntryList(GuiGraphics g, List<GuideData.Entry> entries, int x, int y, int w, int mx, int my) {
		for (GuideData.Entry e : entries) {
			boolean hover = mx >= x && mx < x + w && my >= y && my < y + 18 && inPanel(my);
			if (hover) g.fill(x - 2, y, x + w, y + 18, 0xFFB0B0B0);
			g.renderItem(e.icon(), x, y + 1);
			g.drawString(font, font.plainSubstrByWidth(e.name(), w - 24), x + 21, y + 5, INK, false);
			final GuideData.Entry target = e;
			if (inPanel(y) || inPanel(y + 17)) hits.add(new Hit(x - 2, Math.max(y, top + 8), w + 2, 18, () -> open(target)));
			y += 19;
		}
		return y;
	}

	private boolean inPanel(double y) {
		return y >= top + 8 && y < top + H - 8;
	}

	private int drawEntry(GuiGraphics g, GuideData.Entry e, int x, int y, int w, int mx, int my) {
		// Back link
		boolean hoverBack = mx >= x && mx < x + 40 && my >= y && my < y + 10 && inPanel(my);
		g.drawString(font, "< Back", x, y, hoverBack ? ACCENT : SOFT, false);
		if (inPanel(y)) hits.add(new Hit(x, y, 40, 10, () -> {
			entry = null;
			scroll = 0;
			click();
		}));
		y += 14;

		// Title with a big icon
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(1.5f, 1.5f, 1f);
		g.renderItem(e.icon(), 0, 0);
		g.pose().popPose();
		y = paragraph(g, e.name(), x + 30, y + 8, w - 30, INK);
		y += 6;
		y = paragraph(g, e.text(), x, y, w, INK);
		if (!e.note().isEmpty()) y = paragraph(g, e.note(), x, y, w, SOFT);

		for (String[] stat : e.stats()) {
			g.drawString(font, stat[0] + ": ", x, y, SOFT, false);
			g.drawString(font, stat[1], x + font.width(stat[0] + ": "), y, ACCENT, false);
			y += 10;
		}
		if (!e.stats().isEmpty()) y += 4;

		// Real recipes from the game
		ItemStack target = e.icon();
		List<Recipe<?>> recipes = recipesFor(target);
		if (!recipes.isEmpty() || !e.obtain().isEmpty()) y = heading(g, "How to make it", x, y + 2);
		if (!e.obtain().isEmpty() && recipes.isEmpty()) y = paragraph(g, e.obtain(), x, y, w, INK);
		for (Recipe<?> r : recipes) y = drawRecipe(g, r, x, y, mx, my);

		if (!e.machine().isEmpty()) {
			y = heading(g, "What it processes", x, y + 2);
			y = drawMachineRecipes(g, e.machine(), x, y, w, mx, my);
		}
		return y + 6;
	}

	// ------------------------------------------------------------ recipes
	private List<Recipe<?>> recipesFor(ItemStack target) {
		List<Recipe<?>> out = new ArrayList<>();
		var level = Minecraft.getInstance().level;
		if (level == null) return out;
		var access = level.registryAccess();
		for (var r : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
			if (r.getResultItem(access).is(target.getItem())) out.add(r);
		}
		for (var r : level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
			if (r.getResultItem(access).is(target.getItem())) out.add(r);
		}
		return out;
	}

	private ItemStack cycle(Ingredient ing, int count) {
		ItemStack[] items = ing.getItems();
		if (items.length == 0) return ItemStack.EMPTY;
		long t = System.currentTimeMillis() / 1000;
		return items[(int) (t % items.length)].copyWithCount(count);
	}

	private void slot(GuiGraphics g, ItemStack stack, int x, int y, int mx, int my) {
		g.blit(MachineScreen.WIDGETS, x - 1, y - 1, 0, 0, 18, 18);
		if (stack.isEmpty()) return;
		g.renderItem(stack, x, y);
		g.renderItemDecorations(font, stack, x, y);
		if (mx >= x && mx < x + 16 && my >= y && my < y + 16 && inPanel(my)) hovered = stack;
	}

	private void arrow(GuiGraphics g, int x, int y) {
		g.blit(MachineScreen.WIDGETS, x, y, 24, 32, 24, 17);
	}

	private int drawRecipe(GuiGraphics g, Recipe<?> r, int x, int y, int mx, int my) {
		var access = Minecraft.getInstance().level.registryAccess();
		NonNullList<Ingredient> ings = r.getIngredients();
		if (r instanceof AbstractCookingRecipe) {
			slot(g, cycle(ings.get(0), 1), x + 1, y + 1, mx, my);
			arrow(g, x + 22, y);
			slot(g, r.getResultItem(access), x + 52, y + 1, mx, my);
			g.drawString(font, "Furnace", x + 74, y + 5, SOFT, false);
			return y + 22;
		}
		int w = r instanceof ShapedRecipe s ? s.getWidth() : 3;
		for (int i = 0; i < 9; i++) {
			int col = i % 3, row = i / 3;
			ItemStack stack = ItemStack.EMPTY;
			if (r instanceof ShapedRecipe s) {
				if (col < s.getWidth() && row < s.getHeight()) stack = cycle(ings.get(row * w + col), 1);
			} else if (i < ings.size()) {
				stack = cycle(ings.get(i), 1);
			}
			slot(g, stack, x + 1 + col * 18, y + 1 + row * 18, mx, my);
		}
		arrow(g, x + 60, y + 19);
		slot(g, r.getResultItem(access), x + 90, y + 19, mx, my);
		if (!(r instanceof ShapedRecipe)) g.drawString(font, "(any shape)", x + 112, y + 23, SOFT, false);
		return y + 58;
	}

	private int drawMachineRecipes(GuiGraphics g, String machine, int x, int y, int w, int mx, int my) {
		var level = Minecraft.getInstance().level;
		if (level == null) return y;
		var access = level.registryAccess();
		List<? extends Recipe<?>> list = switch (machine) {
			case "pulverizing" -> level.getRecipeManager().getAllRecipesFor(ModRecipes.PULVERIZING.get());
			case "alloying" -> level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING.get());
			case "purifying" -> level.getRecipeManager().getAllRecipesFor(ModRecipes.PURIFYING.get());
			case "compressing" -> level.getRecipeManager().getAllRecipesFor(ModRecipes.COMPRESSING.get());
			case "sawing" -> level.getRecipeManager().getAllRecipesFor(ModRecipes.SAWING.get());
			case "crushing" -> level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUSHING.get());
			default -> List.of();
		};
		for (Recipe<?> r : list) {
			int cx = x + 1;
			if (r instanceof AlloyingRecipe a) {
				slot(g, cycle(a.getFirst(), a.getFirstCount()), cx, y + 1, mx, my);
				slot(g, cycle(a.getSecond(), a.getSecondCount()), cx + 18, y + 1, mx, my);
				cx += 36;
			} else if (r instanceof MachineRecipe m) {
				slot(g, cycle(m.getIngredient(), m.getCount()), cx, y + 1, mx, my);
				cx += 18;
			} else if (r instanceof PulverizingRecipe p) {
				slot(g, cycle(p.getIngredient(), 1), cx, y + 1, mx, my);
				cx += 18;
			}
			arrow(g, cx + 3, y);
			slot(g, r.getResultItem(access), cx + 31, y + 1, mx, my);
			if (r instanceof MachineRecipe m && !m.getSecondary().isEmpty()) slot(g, m.getSecondary(), cx + 49, y + 1, mx, my);
			y += 19;
		}
		return y;
	}

	// ------------------------------------------------------------ input
	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (super.mouseClicked(mx, my, button)) return true;
		for (Hit h : hits) {
			if (h.contains(mx, my)) {
				h.action().run();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		int ph = H - 16;
		scroll = Math.max(0, Math.min(Math.max(0, contentHeight - ph), scroll - delta * 20));
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		// Backspace goes back a page when the search box isn't being typed in.
		if (key == 259 && !search.isFocused() && entry != null) {
			entry = null;
			scroll = 0;
			return true;
		}
		return super.keyPressed(key, scan, modifiers);
	}

	/** Opens the guide (client only). */
	public static void open() {
		Minecraft.getInstance().setScreen(new GuideScreen());
	}

	@SuppressWarnings("unused")
	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, path);
	}
}
