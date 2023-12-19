import { test, expect } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.goto("http://localhost:3000/providerinput");
});

test("When filling in active ingredients, suggestions are displayed", async ({
  page,
}) => {
  await page.getByPlaceholder("Active Ingredients").click();
  await page.getByPlaceholder("Active Ingredients").fill("b");
  await expect(page.getByText("baclofen")).toBeVisible();
  await expect(page.getByText("bacitracin")).toBeVisible();
  await expect(page.getByText("bactrim", { exact: true })).toBeVisible();
  await page.getByPlaceholder("Active Ingredients").click();
  await page.getByPlaceholder("Active Ingredients").fill("ba");
  await expect(page.getByText("baclofen")).toBeVisible();
  await expect(page.getByText("bacitracin")).toBeVisible();
  await expect(page.getByText("bactrim", { exact: true })).toBeVisible();

  await page.getByPlaceholder("Active Ingredients").click();
  await page.getByPlaceholder("Active Ingredients").fill("a");
  await expect(page.getByText("abilify", { exact: true })).toBeVisible();
  await expect(page.getByText("abilify maintena")).toBeVisible();
  await expect(page.getByText("abiraterone")).toBeVisible();
  await page.getByPlaceholder("Active Ingredients").click();
  await page.getByPlaceholder("Active Ingredients").fill("ab");
  await expect(page.getByText("abilify", { exact: true })).toBeVisible();
  await expect(page.getByText("abilify maintena")).toBeVisible();
  await expect(page.getByText("abiraterone")).toBeVisible();
});

test("When filling in active ingredients, suggestions can be clicked", async ({
  page,
}) => {
  await page.getByPlaceholder("Active Ingredients").click();
  await page.getByPlaceholder("Active Ingredients").fill("b");
  await expect(page.getByText("bacitracin")).toBeVisible();
  await page.getByText("bacitracin").click();

  await page.getByPlaceholder("Active Ingredients").click();
  await page.getByPlaceholder("Active Ingredients").fill("a");
  await expect(page.getByText("abiraterone")).toBeVisible();
  await page.getByText("abiraterone").click();
});

test("When filling in allergies, suggestions are displayed", async ({
  page,
}) => {
  await page.getByPlaceholder("Allergies").click();
  await page.getByPlaceholder("Allergies").fill("b");
  await expect(page.getByText("baclofen")).toBeVisible();
  await expect(page.getByText("bacitracin")).toBeVisible();
  await expect(page.getByText("bactrim", { exact: true })).toBeVisible();
  await page.getByPlaceholder("Allergies").click();
  await page.getByPlaceholder("Allergies").fill("ba");
  await expect(page.getByText("baclofen")).toBeVisible();
  await expect(page.getByText("bacitracin")).toBeVisible();
  await expect(page.getByText("bactrim", { exact: true })).toBeVisible();

  await page.getByPlaceholder("Allergies").click();
  await page.getByPlaceholder("Allergies").fill("a");
  await expect(page.getByText("abilify", { exact: true })).toBeVisible();
  await expect(page.getByText("abilify maintena")).toBeVisible();
  await expect(page.getByText("abiraterone")).toBeVisible();
  await page.getByPlaceholder("Allergies").click();
  await page.getByPlaceholder("Allergies").fill("ab");
  await expect(page.getByText("abilify", { exact: true })).toBeVisible();
  await expect(page.getByText("abilify maintena")).toBeVisible();
  await expect(page.getByText("abiraterone")).toBeVisible();
});

test("When filling in allergies, suggestions can be clicked", async ({
  page,
}) => {
  await page.getByPlaceholder("Allergies").click();
  await page.getByPlaceholder("Allergies").fill("b");
  await expect(page.getByText("bacitracin")).toBeVisible();
  await page.getByText("bacitracin").click();

  await page.getByPlaceholder("Allergies").click();
  await page.getByPlaceholder("Allergies").fill("a");
  await expect(page.getByText("abiraterone")).toBeVisible();
  await page.getByText("abiraterone").click();
});

test("When filling in marketing status, suggestions are displayed", async ({
  page,
}) => {
  await page.getByPlaceholder("Marketing Status").click();
  await page.getByPlaceholder("Marketing Status").fill("p");
  await expect(page.getByText("prescription")).toBeVisible();

  await page.getByPlaceholder("Marketing Status").click();
  await page.getByPlaceholder("Marketing Status").fill("d");
  await expect(page.getByText("discontinued")).toBeVisible();
});

test("When filling in marketing status, suggestions can be clicked", async ({
  page,
}) => {
  await page.getByPlaceholder("Marketing Status").click();
  await page.getByPlaceholder("Marketing Status").fill("p");
  await expect(page.getByText("prescription")).toBeVisible();
  await page.getByText("prescription").click();

  await page.getByPlaceholder("Marketing Status").click();
  await page.getByPlaceholder("Marketing Status").fill("d");
  await expect(page.getByText("discontinued")).toBeVisible();
  await page.getByText("discontinued").click();
});

test("Input active ingredients, allergies, and marketing status", async ({ page }) => {
  await page.getByPlaceholder("Active Ingredients").click();
  await page.getByPlaceholder("Active Ingredients").fill("b");
  await expect(page.getByText("bacitracin")).toBeVisible();
  await page.getByText("bacitracin").click();

  await page.getByPlaceholder("Allergies").click();
  await page.getByPlaceholder("Allergies").fill("a");
  await expect(page.getByText("abiraterone")).toBeVisible();
  await page.getByText("abiraterone").click();

  await page.getByPlaceholder("Marketing Status").click();
  await page.getByPlaceholder("Marketing Status").fill("p");
  await expect(page.getByText("prescription")).toBeVisible();
  await page.getByText("prescription").click();
});

test("On submission, webapp navigates to new page", async ({ page }) => {
  await page.getByPlaceholder("Active Ingredients").click();
  await page.getByPlaceholder("Active Ingredients").fill("b");
  await expect(page.getByText("bacitracin")).toBeVisible();
  await page.getByText("bacitracin").click();

  await page.getByPlaceholder("Allergies").click();
  await page.getByPlaceholder("Allergies").fill("a");
  await expect(page.getByText("abiraterone")).toBeVisible();
  await page.getByText("abiraterone").click();

  await page.getByPlaceholder("Marketing Status").click();
  await page.getByPlaceholder("Marketing Status").fill("p");
  await expect(page.getByText("prescription")).toBeVisible();
  await page.getByText("prescription").click();

  await page.getByRole("button", { name: "Submit" }).click();
  await expect(page.getByText("Search Results")).toBeVisible();
});

test("On back, webapp navigates to home page", async ({ page }) => {
  await page.getByRole("button", { name: "Back" }).click();
  await expect(page.getByText("Clear Answers,")).toBeVisible();
});