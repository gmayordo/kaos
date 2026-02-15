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
