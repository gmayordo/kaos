import "@testing-library/jest-dom";
import { setupServer } from "msw/node";

// Silence console warnings in tests
global.console = {
  ...console,
  error: (...args: any[]) => {
    if (
      typeof args[0] === "string" &&
      (args[0].includes("Not implemented: HTMLFormElement.prototype.submit") ||
        args[0].includes("Not implemented: navigation"))
    ) {
      return;
    }
    console.error(...args);
  },
  warn: (...args: any[]) => {
    if (typeof args[0] === "string" && args[0].includes("act(")) {
      return;
    }
    console.warn(...args);
  },
};

// MSW server setup
export const server = setupServer();

// Establish API mocking before all tests
// beforeAll(() => server.listen({ onUnhandledRequest: "error" }));

// Reset any request handlers that we may add during the tests,
// so they don't affect other tests.
// afterEach(() => server.resetHandlers());

// Clean up after the tests are finished.
// afterAll(() => server.close());
