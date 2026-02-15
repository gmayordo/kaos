import { LogoDisplay } from "@/components";
import { LOGO_TYPES } from "@/lib/logo-manager";

export function LogoShowcase() {
  return (
    <div className="p-8 bg-gray-50 min-h-screen">
      <h1 className="text-4xl font-bold mb-12 text-center">Logo Showcase</h1>

      {/* Sección Random */}
      <section className="mb-16">
        <h2 className="text-2xl font-bold mb-8">Random Logo Pair</h2>
        <div className="bg-white p-8 rounded-lg shadow-lg flex justify-center gap-8">
          <LogoDisplay size={150} />
        </div>
        <p className="text-gray-600 text-center mt-4">
          Los logos se seleccionan aleatoriamente pero del mismo tipo
        </p>
      </section>

      {/* Sección por tipo */}
      <section>
        <h2 className="text-2xl font-bold mb-8">All Logo Types</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {LOGO_TYPES.map((type) => (
            <div key={type} className="bg-white p-8 rounded-lg shadow-lg">
              <h3 className="text-xl font-bold mb-6 capitalize">{type}</h3>
              <div className="flex justify-center gap-8 mb-6">
                <LogoDisplay type={type as any} size={120} />
              </div>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="font-semibold text-gray-700">KAOS</p>
                  <p className="text-gray-500">/logo-kaos-{type}.svg</p>
                </div>
                <div>
                  <p className="font-semibold text-gray-700">CONTROL</p>
                  <p className="text-gray-500">/logo-control-{type}.svg</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Sección de uso */}
      <section className="mt-16 bg-white p-8 rounded-lg shadow-lg">
        <h2 className="text-2xl font-bold mb-6">Usage Examples</h2>
        <div className="space-y-6 text-sm">
          <div className="bg-gray-100 p-4 rounded">
            <p className="font-mono font-semibold mb-2">Random logos:</p>
            <pre className="text-gray-700">{`<LogoDisplay size={100} />`}</pre>
          </div>

          <div className="bg-gray-100 p-4 rounded">
            <p className="font-mono font-semibold mb-2">Tipo específico:</p>
            <pre className="text-gray-700">
              {`<LogoDisplay type="neon" size={100} />`}
            </pre>
          </div>

          <div className="bg-gray-100 p-4 rounded">
            <p className="font-mono font-semibold mb-2">Solo KAOS:</p>
            <pre className="text-gray-700">
              {`<LogoDisplay display="kaos" size={100} />`}
            </pre>
          </div>

          <div className="bg-gray-100 p-4 rounded">
            <p className="font-mono font-semibold mb-2">Solo CONTROL:</p>
            <pre className="text-gray-700">
              {`<LogoDisplay display="control" size={100} />`}
            </pre>
          </div>
        </div>
      </section>
    </div>
  );
}

export default LogoShowcase;
