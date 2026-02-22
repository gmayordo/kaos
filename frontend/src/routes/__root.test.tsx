import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/__root/test')({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/__root/test"!</div>
}
