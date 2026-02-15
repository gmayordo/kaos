import * as logoManager from "@/lib/logo-manager";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LogoDisplay } from "./LogoDisplay";

// Mock logo-manager
vi.mock("@/lib/logo-manager", () => ({
  getRandomLogoPair: vi.fn(() => ({
    kaos: "/logo-kaos-classic.svg",
    control: "/logo-control-classic.svg",
    type: "classic",
  })),
  getLogoPair: vi.fn((type) => ({
    kaos: `/logo-kaos-${type}.svg`,
    control: `/logo-control-${type}.svg`,
    type: type,
  })),
}));

describe("LogoDisplay Component", () => {
  describe("CA-01.1: Renderizar con props por defecto", () => {
    it("must render 2 images (kaos + control) by default", async () => {
      render(<LogoDisplay />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        expect(images).toHaveLength(2);
      });
    });

    it("must have correct alt text", async () => {
      render(<LogoDisplay />);

      await waitFor(() => {
        expect(screen.getByAltText("KAOS Logo")).toBeInTheDocument();
        expect(screen.getByAltText("CONTROL Logo")).toBeInTheDocument();
      });
    });

    it("must have default size of 100px", async () => {
      render(<LogoDisplay />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        images.forEach((img) => {
          expect(img).toHaveStyle({ width: "100px", height: "100px" });
        });
      });
    });

    it("must render with correct title attributes", async () => {
      render(<LogoDisplay />);

      await waitFor(() => {
        const kaosImg = screen.getByAltText("KAOS Logo") as HTMLImageElement;
        const controlImg = screen.getByAltText(
          "CONTROL Logo",
        ) as HTMLImageElement;

        expect(kaosImg.title).toMatch(/KAOS.*classic/);
        expect(controlImg.title).toMatch(/CONTROL.*classic/);
      });
    });
  });

  describe("CA-01.2: Display variants (kaos/control/both)", () => {
    it('display="kaos" must render only KAOS logo', async () => {
      render(<LogoDisplay display="kaos" />);

      await waitFor(() => {
        expect(screen.getByAltText("KAOS Logo")).toBeInTheDocument();
        expect(screen.queryByAltText("CONTROL Logo")).not.toBeInTheDocument();
      });
    });

    it('display="control" must render only CONTROL logo', async () => {
      render(<LogoDisplay display="control" />);

      await waitFor(() => {
        expect(screen.queryByAltText("KAOS Logo")).not.toBeInTheDocument();
        expect(screen.getByAltText("CONTROL Logo")).toBeInTheDocument();
      });
    });

    it('display="both" must render both logos', async () => {
      render(<LogoDisplay display="both" />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        expect(images).toHaveLength(2);
      });
    });
  });

  describe("CA-01.3: Tipo específico", () => {
    it('type="neon" must get neon logos from manager', async () => {
      render(<LogoDisplay type="neon" />);

      await waitFor(() => {
        expect(logoManager.getLogoPair).toHaveBeenCalledWith("neon");
      });
    });

    it('type="vintage" must render vintage logos', async () => {
      render(<LogoDisplay type="vintage" />);

      await waitFor(() => {
        expect(logoManager.getLogoPair).toHaveBeenCalledWith("vintage");
      });
    });

    it("without type prop, must call getRandomLogoPair", async () => {
      render(<LogoDisplay />);

      await waitFor(() => {
        expect(logoManager.getRandomLogoPair).toHaveBeenCalled();
      });
    });
  });

  describe("CA-01.4: Loading state", () => {
    it("should show loading div while loading", () => {
      const { container } = render(<LogoDisplay />);

      // Initially should show loading placeholder
      expect(container.querySelector(".bg-gray-200")).toBeInTheDocument();
    });

    it("should show images after loading completes", async () => {
      render(<LogoDisplay />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        expect(images.length).toBeGreaterThan(0);
      });
    });

    it("loading div should have animate-pulse class", () => {
      const { container } = render(<LogoDisplay />);

      const loadingDiv = container.querySelector(".animate-pulse");
      expect(loadingDiv).toBeInTheDocument();
    });
  });

  describe("CA-01.5: Customización (size, gap, className)", () => {
    it("size={200} must set width and height to 200px", async () => {
      render(<LogoDisplay size={200} />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        images.forEach((img) => {
          expect(img).toHaveStyle({ width: "200px", height: "200px" });
        });
      });
    });

    it("size={150} must set correct pixel size", async () => {
      render(<LogoDisplay size={150} />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        images.forEach((img) => {
          expect(img).toHaveStyle({ width: "150px", height: "150px" });
        });
      });
    });

    it('gap="2rem" must apply flex gap', async () => {
      const { container } = render(<LogoDisplay gap="2rem" />);

      await waitFor(() => {
        const wrapper = container.querySelector('[style*="gap"]');
        expect(wrapper).toHaveStyle({ gap: "2rem" });
      });
    });

    it("className should be applied to wrapper", async () => {
      const { container } = render(<LogoDisplay className="custom-class" />);

      await waitFor(() => {
        expect(container.querySelector(".custom-class")).toBeInTheDocument();
      });
    });

    it("default gap should be 1rem", async () => {
      const { container } = render(<LogoDisplay />);

      await waitFor(() => {
        const wrapper = container.querySelector('[style*="gap"]');
        expect(wrapper).toHaveStyle({ gap: "1rem" });
      });
    });
  });

  describe("Image Sources", () => {
    it("must have valid SVG paths", async () => {
      render(<LogoDisplay />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        images.forEach((img) => {
          const src = (img as HTMLImageElement).src;
          expect(src).toMatch(
            /logo-(kaos|control)-(classic|modern|neon|geometric|vintage|icon)\.svg/,
          );
        });
      });
    });

    it("must load images with objectFit contain", async () => {
      render(<LogoDisplay />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        images.forEach((img) => {
          expect(img).toHaveStyle({ objectFit: "contain" });
        });
      });
    });
  });

  describe("Edge Cases", () => {
    it("should handle size prop as 0 gracefully", async () => {
      render(<LogoDisplay size={0} />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        expect(images.length).toBeGreaterThan(0);
      });
    });

    it("should handle very large size prop", async () => {
      render(<LogoDisplay size={1000} />);

      await waitFor(() => {
        const images = screen.getAllByRole("img");
        images.forEach((img) => {
          expect(img).toHaveStyle({ width: "1000px", height: "1000px" });
        });
      });
    });

    it("should handle empty className", async () => {
      render(<LogoDisplay className="" />);

      await waitFor(() => {
        expect(screen.getByAltText("KAOS Logo")).toBeInTheDocument();
      });
    });
  });

  describe("Accessibility", () => {
    it("must have alt text for images", async () => {
      render(<LogoDisplay />);

      await waitFor(() => {
        const kaosImg = screen.getByAltText("KAOS Logo");
        const controlImg = screen.getByAltText("CONTROL Logo");

        expect(kaosImg).toHaveAttribute("alt");
        expect(controlImg).toHaveAttribute("alt");
      });
    });

    it("must have title attribute for additional context", async () => {
      render(<LogoDisplay type="neon" />);

      await waitFor(() => {
        const kaosImg = screen.getByAltText("KAOS Logo");
        const controlImg = screen.getByAltText("CONTROL Logo");

        expect(kaosImg).toHaveAttribute("title");
        expect(controlImg).toHaveAttribute("title");
      });
    });
  });
});
